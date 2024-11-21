package tradingengine.models;

import java.sql.Timestamp;
import java.util.UUID;


public class Order {
    public enum Side { BUY, SELL }
    public enum OrderType { MARKET, LIMIT }
    // public enum OrderStatus { NEW, FILLED, PARTIALLY_FILLED, CANCELLED }

    private UUID orderId;
    private String symbol; // Trading symbol/instrument
    private String userId;
    public String side;
    public OrderType orderType;
    private long price;
    public int currentQuantity;
    public int originalQuantity;
    public Timestamp timestamp;
    // public OrderStatus status;

    public Order(UUID orderId, String symbol, String userId, String side, OrderType orderType, int price, int quantity, Timestamp timestamp) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.userId = userId;
        this.side = side;
        this.orderType = orderType;
        this.price = price;
        this.currentQuantity = quantity;
        this.originalQuantity = quantity;
        this.timestamp = timestamp;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getUserId() {
        return userId;
    }

    public long getPrice() {
        return price;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public int getCurrentQuantity() {
        return currentQuantity;
    }

    public int getOriginalQuantity() {
        return originalQuantity;
    }
}
