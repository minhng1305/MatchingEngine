package com.project.matchingengine.service.authentication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.matchingengine.models.authentication.Portfolio;
import com.project.matchingengine.models.authentication.User;
import com.project.matchingengine.repository.authentication.PortfolioRepo;
import com.project.matchingengine.repository.authentication.UserRepo;


/*
 * In-memory cache for user balances and portfolios.
 * This is the only service that accesses the database directly.
 * Other services should use this service to fetch from the cache instead of directly accessing the database.
 */
// TODO: Update such that when cache updates to database, it should maintained a fixed-size trade record in the cache to fetch to frontend for real-time trading
@Service
public class UserDetailsCacheService {
    private static final Logger logger = LoggerFactory.getLogger(UserDetailsCacheService.class);

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private PortfolioRepo portfolioRepo;

    private final ConcurrentHashMap<UUID, CachedUserDetails> cache = new ConcurrentHashMap<>();
    private final Set<UUID> dirtyUsers = Collections.synchronizedSet(new HashSet<>());
    private static final int MAX_CACHE_SIZE = 10_000;  // Prevent memory explosion
    private static final long EVICTION_TIME_MS = 1800_000;  // 30-min inactivity

    public static class CachedUserDetails {
        public UUID userId;
        public double ledgerBalance;
        public double availableBalance;
        public Map<String, Integer> holdings;
        public long lastAccessTime;
        public boolean isDirty;

        public CachedUserDetails(UUID userId, double ledger, double available,
                                 Map<String, Integer> holdings) {
            this.userId = userId;
            this.ledgerBalance = ledger;
            this.availableBalance = available;
            this.holdings = new ConcurrentHashMap<>(holdings);
            this.lastAccessTime = System.currentTimeMillis();
            this.isDirty = false;
        }

        public synchronized void applyTrade(String symbol, int quantityDelta, double tradePrice, double initialPrice, boolean isBuy) {
            if (isBuy) {
                this.availableBalance += quantityDelta * initialPrice - quantityDelta * tradePrice;
                this.ledgerBalance -= quantityDelta * initialPrice;
                this.holdings.put(symbol, this.holdings.getOrDefault(symbol, 0) + quantityDelta);
            } else {
                this.ledgerBalance += quantityDelta * tradePrice;
                this.availableBalance += quantityDelta * tradePrice;
            }
            this.lastAccessTime = System.currentTimeMillis();
            this.isDirty = true;
        }

        public synchronized boolean hasAvailableFunds(double amount) {
            this.lastAccessTime = System.currentTimeMillis();
            return this.availableBalance >= amount;
        }

        public synchronized boolean hasAvailableStock(String symbol, int quantity) {
            this.lastAccessTime = System.currentTimeMillis();
            return this.holdings.getOrDefault(symbol, 0) >= quantity;
        }
    }

    /**
     * LAZY LOAD - Get user balance (load from DB if not cached)
     */
    public CachedUserDetails getBalance(UUID userId) {
        // Check cache first (fast path)
        CachedUserDetails cached = cache.get(userId);
        if (cached != null) {
            cached.lastAccessTime = System.currentTimeMillis();
            return cached;
        }
        // Cache miss - load from DB (slow path, happens once)
        return loadUserIntoCache(userId);
    }

    /**
     * Load user from DB into cache (on-demand)
     */
    private CachedUserDetails loadUserIntoCache(UUID userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        // Fetch portfolio
        Map<String, Integer> holdings = portfolioRepo.findByUserId(userId)
                .stream()
                .collect(Collectors.toMap(
                        Portfolio::getSymbol,
                        Portfolio::getQuantity,
                        Integer::sum  // Handle duplicates
                ));
        CachedUserDetails balance = new CachedUserDetails(
                userId,
                user.getLedgerBalance(),
                user.getAvailableBalance(),
                holdings
        );

        if (cache.size() < MAX_CACHE_SIZE) {
            cache.put(userId, balance);
        } else {  // Cache full -> evict the oldest entry
            evictOldestEntry();
            cache.put(userId, balance);
        }
        System.out.println("User " + userId + " loaded into cache (total: " + cache.size() + ")");
        return balance;
    }

    /**
     * Apply trade immediately to cache (in-memory update)
     */
    public void applyTrade(UUID userId, String symbol, int quantityDelta, double tradePrice, double initialPrice, boolean isBuy) {
        CachedUserDetails balance = getBalance(userId);  // Ensure in cache
        balance.applyTrade(symbol, quantityDelta, tradePrice, initialPrice, isBuy);
        dirtyUsers.add(userId);
    }

    /**
     * Check if user can place order (in-memory check, no DB call)
     */
    public void placeOrder(UUID userId, String symbol, int quantity, double price, boolean isBuy) {
        CachedUserDetails balance = getBalance(userId);
        if (isBuy) {
            if (!balance.hasAvailableFunds(price * quantity)) {
                throw new RuntimeException("Insufficient funds");
            }
            balance.availableBalance -= price * quantity;
        } else {
            if (!balance.hasAvailableStock(symbol, quantity)) {
               throw new RuntimeException("Insufficient stock holdings");
            }
            balance.holdings.put(symbol, balance.holdings.get(symbol) - quantity);
        }
        dirtyUsers.add(userId);
    }

    /**
     * Get all cached users (for batch persistence)
     */
    public Map<UUID, CachedUserDetails> getAllCachedBalances() {
        return new HashMap<>(cache);
    }

    /**
     * Evict inactive users from cache (memory management)
     */
    // TODO: When entry is removed, should update the entry data onto DB first before removing it from cache
    private void evictOldestEntry() {
        // Find and remove oldest entry
        cache.entrySet().stream()
                .min(Comparator.comparingLong(e -> e.getValue().lastAccessTime))
                .ifPresent(entry -> {
                    cache.remove(entry.getKey());

                    System.out.println("Evicted user " + entry.getKey() + " from cache");
                });
    }

    /**
     * Background task to clean up stale cache entries
     */
    // TODO: When entry is removed, should update the entry data onto DB first before removing it from cache
    // TODO: Verify if the updates are correctly applied to the DB
    @Scheduled(fixedRate = 300000)  // Every 5 mins
    public void evictStaleEntries() {
        long now = System.currentTimeMillis();
        int evicted = 0;

        for (Map.Entry<UUID, CachedUserDetails> entry : cache.entrySet()) {
            if (now - entry.getValue().lastAccessTime > EVICTION_TIME_MS) {
                cache.remove(entry.getKey());
                evicted++;
            }
        }
        if (evicted > 0) {
            System.out.println("Cache cleanup: evicted " + evicted +
                    " inactive users (remaining: " + cache.size() + ")");
        }
    }

    public Set<UUID> getDirtyUsers() {
        return new HashSet<>(dirtyUsers);
    }

    private void clearDirtyUsers() {
        dirtyUsers.clear();
    }

    /*
     * Background task to update changes to all schemas inside DB
     */
    // TODO: Update all schemas inside DB
    @Scheduled(fixedRate = 5000) // Every 5 seconds
    @Transactional
    public void updateDatabase() {
        try {
            if (dirtyUsers.isEmpty()) {
                logger.debug("No dirty users, skipping DB update");
                return;
            }

            logger.info("Starting batch update for {} dirty users...", dirtyUsers.size());

            // Step 1: Update Users table
            List<User> usersToUpdate = new ArrayList<>();
            for (UUID userId : dirtyUsers) {
                CachedUserDetails cached = cache.get(userId);
                if (cached != null) {
                    User user = userRepo.findById(userId).orElse(null);
                    if (user != null) {
                        user.setLedgerBalance(cached.ledgerBalance);
                        user.setAvailableBalance(cached.availableBalance);
                        usersToUpdate.add(user);
                    }
                }
            }
            userRepo.saveAll(usersToUpdate);
            logger.info("Updated {} user records", usersToUpdate.size());

            // Step 2: Update Portfolio table
            // TODO: Remove the database call for the portfolio table
            List<Portfolio> portfoliosToUpdate = new ArrayList<>();
            List<Portfolio> portfoliosToDelete = new ArrayList<>();
            
            for (UUID userId : dirtyUsers) {
                CachedUserDetails cached = cache.get(userId);
                if (cached != null) {
                    for (Map.Entry<String, Integer> holding : cached.holdings.entrySet()) {
                        String symbol = holding.getKey();
                        int newQuantity = holding.getValue();
                        
                        // Check if portfolio exists in database using the composite key
                        Optional<Portfolio> existingPortfolioOpt = portfolioRepo.findByUserIdAndSymbol(userId, symbol);
                        
                        if (existingPortfolioOpt.isPresent()) {
                            // Portfolio exists - update it
                            Portfolio portfolio = existingPortfolioOpt.get();
                            int oldQuantity = portfolio.getQuantity();
                            
                            if (oldQuantity != newQuantity) {
                                if (newQuantity > 0) {
                                    portfolio.setQuantity(newQuantity);
                                    portfoliosToUpdate.add(portfolio);
                                } else {
                                    // Delete portfolios with zero quantity
                                    portfoliosToDelete.add(portfolio);
                                }
                            }
                        } else {
                            // Portfolio doesn't exist - create new one only if quantity > 0
                            if (newQuantity > 0) {
                                Portfolio newPortfolio = new Portfolio(userId, symbol, newQuantity);
                                portfoliosToUpdate.add(newPortfolio);
                            }
                            // If quantity is 0 and it doesn't exist, do nothing
                        }
                    }
                }
            }
            
            // Save/Insert new and updated portfolios
            if (!portfoliosToUpdate.isEmpty()) {
                portfolioRepo.saveAll(portfoliosToUpdate);
                logger.info("Updated/Inserted {} portfolio records", portfoliosToUpdate.size());
            }

            // Step 3: Clear dirty tracking
            clearDirtyUsers();
            logger.info("✅ Batch update complete!");
        } catch (Exception e) {
            logger.error("❌ CRITICAL ERROR in updateDatabase(): {}", e.getMessage(), e);
            // TODO: Add dead letter queue / retry mechanism
        }
    }

    public void invalidate(UUID userId) {
        cache.remove(userId);
    }

    public int getCacheSize() {
        return cache.size();
    }
}
