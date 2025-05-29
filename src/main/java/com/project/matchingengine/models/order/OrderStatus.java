package com.project.matchingengine.models.order;

import java.io.Serializable;

public enum OrderStatus implements Serializable {
    PENDING,
    FILLED,
    PARTIALLY_FILLED,
    CANCELED;

    private static final long serialVersionUID = 1L;

    public static OrderStatus fromString(String status) {
        if (status == null) {
            return null;
        }
        switch (status.toUpperCase()) {
            case "PENDING":
                return PENDING;
            case "FILLED":
                return FILLED;
            case "PARTIALLY_FILLED":
                return PARTIALLY_FILLED;
            case "CANCELED":
                return CANCELED;
            default:
                throw new IllegalArgumentException("Unknown order status: " + status);
        }
    }
}