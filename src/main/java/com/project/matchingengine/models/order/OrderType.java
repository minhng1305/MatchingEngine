package com.project.matchingengine.models.order;


public enum OrderType implements java.io.Serializable {
    LIMIT,
    MARKET;

    private static final long serialVersionUID = 1L;

    public static OrderType fromString(String type) {
        if (type == null) {
            return null;
        }
        switch (type.toUpperCase()) {
            case "LIMIT":
                return LIMIT;
            case "MARKET":
                return MARKET;
            default:
                throw new IllegalArgumentException("Unknown order type: " + type);
        }
    }
}