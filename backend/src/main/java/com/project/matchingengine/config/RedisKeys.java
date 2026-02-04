package com.project.matchingengine.config;

import java.util.UUID;


public class RedisKeys {
    private static final String PREFIX = "me:";

    public static String userBalance(UUID userId) {
        return PREFIX + "user:" + userId + ":balance";
    }

    public static String userHolding(UUID userId, String symbol) {
        return PREFIX + "user:" + userId + ":h:" + symbol.toUpperCase();
    }

    public static String dirtyUsers() {
        return PREFIX + "dirty:users";
    }

    public static String dbSyncLock() {
        return PREFIX + "lock:db-sync";
    }
}
