package com.project.matchingengine.config;

import java.util.UUID;

/**
 * Redis key constants for consistent key naming across the application
 */
public class RedisKeys {
    private static final String PREFIX = "me:";
    
    /**
     * User balance hash key
     * Hash fields: ledger, available, updatedAt
     */
    public static String userBalance(UUID userId) {
        return PREFIX + "user:" + userId + ":balance";
    }
    
    /**
     * User holding key (quantity for a specific symbol)
     */
    public static String userHolding(UUID userId, String symbol) {
        return PREFIX + "user:" + userId + ":h:" + symbol.toUpperCase();
    }
    
    /**
     * Dirty users set (users that need DB sync)
     */
    public static String dirtyUsers() {
        return PREFIX + "dirty:users";
    }
    
    /**
     * Lock key for DB sync (to ensure only one server syncs at a time)
     */
    public static String dbSyncLock() {
        return PREFIX + "lock:db-sync";
    }
}
