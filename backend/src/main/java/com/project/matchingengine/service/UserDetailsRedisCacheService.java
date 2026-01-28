package com.project.matchingengine.service;


import com.project.matchingengine.config.RedisKeys;
import com.project.matchingengine.models.authentication.Portfolio;
import com.project.matchingengine.models.authentication.User;
import com.project.matchingengine.repository.authentication.PortfolioRepo;
import com.project.matchingengine.repository.authentication.UserRepo;
import com.project.matchingengine.service.authentication.RedisLuaScripts;
import com.project.matchingengine.service.authentication.UserDetailsCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// TODO: Implement Redis Caching for User Details
@Service
public class UserDetailsRedisCacheService {
    private static final Logger logger = LoggerFactory.getLogger(UserDetailsRedisCacheService.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private PortfolioRepo portfolioRepo;

    // Lua scripts for atomic operations
    private DefaultRedisScript<String> applyBuyTradeScript;
    private DefaultRedisScript<String> applySellTradeScript;
    private DefaultRedisScript<String> placeBuyOrderScript;
    private DefaultRedisScript<String> placeSellOrderScript;

    private static final long EVICTION_TIME_MS = 1800_000;  // 30-min inactivity
    private static final long DB_SYNC_LOCK_TIMEOUT = 10;  // 10 seconds lock timeout

    /**
     * Initialize Lua scripts on service creation
     */
    @Autowired
    public void initLuaScripts() {
        this.applyBuyTradeScript = new DefaultRedisScript<>();
        this.applyBuyTradeScript.setScriptText(RedisLuaScripts.APPLY_BUY_TRADE);
        this.applyBuyTradeScript.setResultType(String.class);

        this.applySellTradeScript = new DefaultRedisScript<>();
        this.applySellTradeScript.setScriptText(RedisLuaScripts.APPLY_SELL_TRADE);
        this.applySellTradeScript.setResultType(String.class);

        this.placeBuyOrderScript = new DefaultRedisScript<>();
        this.placeBuyOrderScript.setScriptText(RedisLuaScripts.PLACE_BUY_ORDER);
        this.placeBuyOrderScript.setResultType(String.class);

        this.placeSellOrderScript = new DefaultRedisScript<>();
        this.placeSellOrderScript.setScriptText(RedisLuaScripts.PLACE_SELL_ORDER);
        this.placeSellOrderScript.setResultType(String.class);
    }

    /**
     * Cached user details - in-memory DTO for backward compatibility
     * Data is stored in Redis, this class is just for reading/writing
     */
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
     * Get user balance from Redis (load from DB if not in Redis)
     */
    public CachedUserDetails getBalance(UUID userId) {
        String balanceKey = RedisKeys.userBalance(userId);

        // Check Redis first (fast path)
        Map<Object, Object> balanceHash = stringRedisTemplate.opsForHash().entries(balanceKey);

        if (balanceHash != null && !balanceHash.isEmpty()) {
            // Found in Redis - build CachedUserDetails from Redis data
            double ledger = Double.parseDouble(String.valueOf(balanceHash.get("ledger")));
            double available = Double.parseDouble(String.valueOf(balanceHash.get("available")));

            // Load holdings from Redis
            Map<String, Integer> holdings = loadHoldingsFromRedis(userId);

            CachedUserDetails cached = new CachedUserDetails(userId, ledger, available, holdings);
            return cached;
        }

        // Cache miss - load from DB and store in Redis
        return loadUserIntoRedis(userId);
    }

    /**
     * Load holdings from Redis for a user
     */
    private Map<String, Integer> loadHoldingsFromRedis(UUID userId) {
        Map<String, Integer> holdings = new HashMap<>();

        // Get all holding keys for this user: me:user:{userId}:h:*
        String pattern = "me:user:" + userId + ":h:*";
        Set<String> keys = stringRedisTemplate.keys(pattern);

        if (keys != null) {
            for (String key : keys) {
                // Extract symbol from key: me:user:{userId}:h:{symbol}
                String symbol = key.substring(key.lastIndexOf(":") + 1);
                String quantityStr = stringRedisTemplate.opsForValue().get(key);
                if (quantityStr != null) {
                    holdings.put(symbol, Integer.parseInt(quantityStr));
                }
            }
        }

        return holdings;
    }

    /**
     * Load user from DB into Redis (on-demand)
     */
    private CachedUserDetails loadUserIntoRedis(UUID userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Fetch portfolio from DB
        Map<String, Integer> holdings = portfolioRepo.findByUserId(userId)
                .stream()
                .collect(Collectors.toMap(
                        Portfolio::getSymbol,
                        Portfolio::getQuantity,
                        Integer::sum  // Handle duplicates
                ));

        // Store balance in Redis
        String balanceKey = RedisKeys.userBalance(userId);
        Map<String, String> balanceHash = new HashMap<>();
        balanceHash.put("ledger", String.valueOf(user.getLedgerBalance()));
        balanceHash.put("available", String.valueOf(user.getAvailableBalance()));
        balanceHash.put("updatedAt", String.valueOf(System.currentTimeMillis()));
        stringRedisTemplate.opsForHash().putAll(balanceKey, balanceHash);

        // Store holdings in Redis
        for (Map.Entry<String, Integer> entry : holdings.entrySet()) {
            String holdingKey = RedisKeys.userHolding(userId, entry.getKey());
            stringRedisTemplate.opsForValue().set(holdingKey, String.valueOf(entry.getValue()));
        }

        logger.debug("User {} loaded into Redis cache", userId);

        return new CachedUserDetails(
                userId,
                user.getLedgerBalance(),
                user.getAvailableBalance(),
                holdings
        );
    }

    /**
     * Apply trade atomically to Redis using Lua script
     */
    public void applyTrade(UUID userId, String symbol, int quantityDelta, double tradePrice, double initialPrice, boolean isBuy) {
        // Ensure user exists in Redis (load from DB if not)
        String balanceKey = RedisKeys.userBalance(userId);
        if (!stringRedisTemplate.hasKey(balanceKey)) {
            loadUserIntoRedis(userId);
        }

        String holdingKey = RedisKeys.userHolding(userId, symbol);
        String updatedAt = String.valueOf(System.currentTimeMillis());

        List<String> keys = List.of(balanceKey, holdingKey);
        List<String> args;
        DefaultRedisScript<String> script;

        if (isBuy) {
            args = List.of(
                    String.valueOf(quantityDelta),
                    String.valueOf(tradePrice),
                    String.valueOf(initialPrice),
                    updatedAt
            );
            script = applyBuyTradeScript;
        } else {
            args = List.of(
                    String.valueOf(quantityDelta),
                    String.valueOf(tradePrice),
                    updatedAt
            );
            script = applySellTradeScript;
        }

        String result = stringRedisTemplate.execute(script, keys, args.toArray(new Object[0]));

        if (!"OK".equals(result)) {
            throw new RuntimeException("Failed to apply trade in Redis: " + result);
        }

        // Mark user as dirty for DB sync
        stringRedisTemplate.opsForSet().add(RedisKeys.dirtyUsers(), userId.toString());

        logger.debug("Applied {} trade for user {}: {} {} @ {}",
                isBuy ? "BUY" : "SELL", userId, quantityDelta, symbol, tradePrice);
    }

    /**
     * Check if user can place order and deduct atomically using Lua script
     */
    public void placeOrder(UUID userId, String symbol, int quantity, double price, boolean isBuy) {
        // Ensure user exists in Redis
        String balanceKey = RedisKeys.userBalance(userId);
        if (!stringRedisTemplate.hasKey(balanceKey)) {
            loadUserIntoRedis(userId);
        }

        String updatedAt = String.valueOf(System.currentTimeMillis());
        List<String> keys;
        List<String> args;
        DefaultRedisScript<String> script;

        if (isBuy) {
            keys = List.of(balanceKey);
            args = List.of(String.valueOf(price * quantity), updatedAt);
            script = placeBuyOrderScript;
        } else {
            String holdingKey = RedisKeys.userHolding(userId, symbol);
            keys = List.of(holdingKey);
            args = List.of(String.valueOf(quantity));
            script = placeSellOrderScript;
        }

        String result = stringRedisTemplate.execute(script, keys, args.toArray(new Object[0]));

        if ("INSUFFICIENT".equals(result)) {
            if (isBuy) {
                throw new RuntimeException("Insufficient funds");
            } else {
                throw new RuntimeException("Insufficient stock holdings");
            }
        }

        if (!"OK".equals(result)) {
            throw new RuntimeException("Failed to place order in Redis: " + result);
        }

        // Mark user as dirty for DB sync
        stringRedisTemplate.opsForSet().add(RedisKeys.dirtyUsers(), userId.toString());

        logger.debug("Placed {} order for user {}: {} {} @ {}",
                isBuy ? "BUY" : "SELL", userId, quantity, symbol, price);
    }

    /**
     * Get all cached users (for batch persistence)
     * Note: This method is less efficient with Redis as we need to scan keys
     * Consider using this only for debugging/monitoring
     */
    public Map<UUID, UserDetailsCacheService.CachedUserDetails> getAllCachedBalances() {
        Map<UUID, UserDetailsCacheService.CachedUserDetails> result = new HashMap<>();

        // Get all balance keys: me:user:*:balance
        Set<String> balanceKeys = stringRedisTemplate.keys("me:user:*:balance");

        if (balanceKeys != null) {
            for (String key : balanceKeys) {
                // Extract userId from key: me:user:{userId}:balance
                try {
                    int userStart = key.indexOf("user:") + 5;
                    int userEnd = key.lastIndexOf(":balance");
                    String userIdStr = key.substring(userStart, userEnd);
                    UUID userId = UUID.fromString(userIdStr);
                    result.put(userId, getBalance(userId));
                } catch (IllegalArgumentException | StringIndexOutOfBoundsException e) {
                    logger.warn("Invalid userId in Redis key: {}", key);
                }
            }
        }

        return result;
    }

    /**
     * Background task to clean up stale cache entries from Redis
     * Evicts users that haven't been accessed in EVICTION_TIME_MS
     */
    @Scheduled(fixedRate = 300000)  // Every 5 mins
    public void evictStaleEntries() {
        long now = System.currentTimeMillis();
        int evicted = 0;

        // Get all balance keys: me:user:*:balance
        Set<String> balanceKeys = stringRedisTemplate.keys("me:user:*:balance");

        if (balanceKeys != null) {
            for (String key : balanceKeys) {
                String updatedAtStr = (String) stringRedisTemplate.opsForHash().get(key, "updatedAt");
                if (updatedAtStr != null) {
                    try {
                        long updatedAt = Long.parseLong(updatedAtStr);
                        if (now - updatedAt > EVICTION_TIME_MS) {
                            // Extract userId and delete all related keys
                            int userStart = key.indexOf("user:") + 5;
                            int userEnd = key.lastIndexOf(":balance");
                            String userIdStr = key.substring(userStart, userEnd);
                            UUID userId = UUID.fromString(userIdStr);
                            invalidate(userId);
                            evicted++;
                        }
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid number format in Redis key: {}", key);
                    } catch (IllegalArgumentException | StringIndexOutOfBoundsException e) {
                        logger.warn("Invalid userId in Redis key: {}", key);
                        logger.warn("Invalid updatedAt or userId in Redis key: {}", key);
                    }
                }
            }
        }

        if (evicted > 0) {
            logger.info("Cache cleanup: evicted {} inactive users from Redis", evicted);
        }
    }

    /**
     * Get dirty users from Redis set
     */
    public Set<UUID> getDirtyUsers() {
        Set<String> dirtyUserStrs = stringRedisTemplate.opsForSet().members(RedisKeys.dirtyUsers());
        if (dirtyUserStrs == null) {
            return new HashSet<>();
        }
        return dirtyUserStrs.stream()
                .map(UUID::fromString)
                .collect(Collectors.toSet());
    }

    private void clearDirtyUsers() {
        stringRedisTemplate.delete(RedisKeys.dirtyUsers());
    }

    /**
     * Background task to sync Redis cache to database
     * Uses distributed lock to ensure only one server syncs at a time
     */
    @Scheduled(fixedRate = 5000) // Every 5 seconds
    @Transactional
    public void updateDatabase() {
        try {
            // Try to acquire distributed lock (only one server should sync)
            String lockKey = RedisKeys.dbSyncLock();
            Boolean lockAcquired = stringRedisTemplate.opsForValue().setIfAbsent(
                    lockKey,
                    "locked",
                    java.time.Duration.ofSeconds(DB_SYNC_LOCK_TIMEOUT)
            );

            if (lockAcquired == null || !lockAcquired) {
                logger.debug("DB sync lock not acquired, another server is syncing");
                return;
            }

            try {
                Set<UUID> dirtyUsers = getDirtyUsers();

                if (dirtyUsers.isEmpty()) {
                    logger.debug("No dirty users, skipping DB update");
                    return;
                }

                logger.info("Starting batch update for {} dirty users from Redis...", dirtyUsers.size());

                // Step 1: Update Users table from Redis
                List<User> usersToUpdate = new ArrayList<>();
                for (UUID userId : dirtyUsers) {
                    UserDetailsCacheService.CachedUserDetails cached = getBalance(userId);  // Read from Redis
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

                // Step 2: Update Portfolio table from Redis
                List<Portfolio> portfoliosToUpdate = new ArrayList<>();
                List<Portfolio> portfoliosToDelete = new ArrayList<>();

                for (UUID userId : dirtyUsers) {
                    UserDetailsCacheService.CachedUserDetails cached = getBalance(userId);  // Read from Redis
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
                            }
                        }
                    }
                }

                // Save/Insert new and updated portfolios
                if (!portfoliosToUpdate.isEmpty()) {
                    portfolioRepo.saveAll(portfoliosToUpdate);
                    logger.info("Updated/Inserted {} portfolio records", portfoliosToUpdate.size());
                }

                if (!portfoliosToDelete.isEmpty()) {
                    portfolioRepo.deleteAll(portfoliosToDelete);
                    logger.info("Deleted {} portfolio records with zero quantity", portfoliosToDelete.size());
                }

                // Step 3: Clear dirty tracking
                clearDirtyUsers();
                logger.info("✅ Batch update complete!");
            } finally {
                // Release lock
                stringRedisTemplate.delete(lockKey);
            }
        } catch (Exception e) {
            logger.error("❌ CRITICAL ERROR in updateDatabase(): {}", e.getMessage(), e);
            // TODO: Add dead letter queue / retry mechanism
        }
    }

    /**
     * Invalidate user cache in Redis (delete all keys for this user)
     */
    public void invalidate(UUID userId) {
        // Delete balance
        stringRedisTemplate.delete(RedisKeys.userBalance(userId));

        // Delete all holdings: me:user:{userId}:h:*
        String pattern = "me:user:" + userId + ":h:*";
        Set<String> holdingKeys = stringRedisTemplate.keys(pattern);
        if (holdingKeys != null && !holdingKeys.isEmpty()) {
            stringRedisTemplate.delete(holdingKeys);
        }

        // Remove from dirty set
        stringRedisTemplate.opsForSet().remove(RedisKeys.dirtyUsers(), userId.toString());

        logger.debug("Invalidated Redis cache for user {}", userId);
    }

    /**
     * Get approximate cache size (number of users in Redis)
     * Note: This is less efficient with Redis as we need to scan keys
     */
    public int getCacheSize() {
        Set<String> balanceKeys = stringRedisTemplate.keys("me:user:*:balance");
        return balanceKeys != null ? balanceKeys.size() : 0;
    }

}


