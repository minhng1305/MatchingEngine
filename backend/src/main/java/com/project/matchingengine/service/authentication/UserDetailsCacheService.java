package com.project.matchingengine.service.authentication;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import com.project.matchingengine.repository.authentication.UserRepo;
import com.project.matchingengine.repository.authentication.PortfolioRepo;
import com.project.matchingengine.models.authentication.User;
import com.project.matchingengine.models.authentication.Portfolio;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/*
 * In-memory cache for user balances and portfolios.
 * This is the only service that accesses the database directly.
 * Other services should use this service to fetch from the cache instead of directly accessing the database.
 */
@Service
public class UserDetailsCacheService {
    @Autowired
    private UserRepo userRepo;

    @Autowired
    private PortfolioRepo portfolioRepo;

    private final ConcurrentHashMap<UUID, CachedUserDetails> cache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 10_000;  // Prevent memory explosion
    private static final long EVICTION_TIME_MS = 1800_000;  // 30-min inactivity

    public static class CachedUserDetails {
        public UUID userId;
        public double ledgerBalance;
        public double availableBalance;
        public Map<String, Integer> holdings;
        public long lastAccessTime;

        public CachedUserDetails(UUID userId, double ledger, double available,
                                 Map<String, Integer> holdings) {
            this.userId = userId;
            this.ledgerBalance = ledger;
            this.availableBalance = available;
            this.holdings = new ConcurrentHashMap<>(holdings);
            this.lastAccessTime = System.currentTimeMillis();
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
        // Cache miss - load from database (slow path, happens once)
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

        // Create cache entry
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
    private void evictOldestEntry() {
        long now = System.currentTimeMillis();
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
    @Scheduled(fixedRate = 300000)  // Every 5 minutes
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

    public void invalidate(UUID userId) {
        cache.remove(userId);
    }

    public int getCacheSize() {
        return cache.size();
    }
}
