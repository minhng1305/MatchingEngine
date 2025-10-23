package com.project.matchingengine.models.order;

public enum OrderStatus{
    PENDING,
    FILLED,
    PARTIALLY_FILLED,
    CANCELED;


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
            case "CANCELLED":
                return CANCELED;
            default:
                throw new IllegalArgumentException("Unknown order status: " + status);
        }
    }
}