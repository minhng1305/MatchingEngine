package com.project.matchingengine.models.order;


public enum OrderSide implements java.io.Serializable {
    BUY,
    SELL;

    private static final long serialVersionUID = 1L;

    public static OrderSide fromString(String side) {
        if (side == null) {
            return null;
        }
        switch (side.toUpperCase()) {
            case "BUY":
                return BUY;
            case "SELL":
                return SELL;
            default:
                throw new IllegalArgumentException("Unknown order side: " + side);
        }
    }
}