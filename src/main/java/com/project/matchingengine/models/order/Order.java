package com.project.matchingengine.models.order;

import java.sql.Timestamp;
import java.util.UUID;


public class Order implements java.io.Serializable {    

    private static final long serialVersionUID = 1L;
    private UUID orderId;
    private String userId;
    private String symbol;
    private double price;
    private int originalQuantity;
    public int currentQuantity;
    private OrderSide side;
    private OrderType type;
    private double limitPrice;
    private Timestamp orderTimestamp;
    public OrderStatus status;


    public Order() {
        // Default constructor for serialization/deserialization
    }
    
    public Order(UUID orderId,
                 String userId,
                 String symbol, 
                 double price, 
                 int originalQuantity, 
                 OrderSide side, 
                 OrderType type, 
                 double limitPrice, 
                 Timestamp orderTimestamp) {
        this.orderId = orderId;
        this.userId = userId;
        this.symbol = symbol;
        this.price = price;
        this.currentQuantity = originalQuantity;
        this.originalQuantity = originalQuantity;
        this.side = side;
        this.type = type;
        this.limitPrice = limitPrice; // if type == MARKET, this will be 0; else, it will be the limit price
        this.orderTimestamp = orderTimestamp;
        this.status = OrderStatus.PENDING;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
    
    public int getOriginalQuantity() {
        return originalQuantity;
    }

    public void setOriginalQuantity(int originalQuantity) {
        this.originalQuantity = originalQuantity;
    }

    public OrderSide getSide() {
        return side;
    }

    public void setSide(OrderSide side) {
        this.side = side;
    }

    public OrderType getType() {
        return type;
    }

    public void setType(OrderType type) {
        this.type = type;
    }

    public double getLimitPrice() {
        return limitPrice;
    }

    public void setLimitPrice(double limitPrice) {
        this.limitPrice = limitPrice;
    }

    public Timestamp getOrderTimestamp() {
        return orderTimestamp;
    }

    public void setOrderTimestamp(Timestamp orderTimestamp) {
        this.orderTimestamp = orderTimestamp;
    }
}
