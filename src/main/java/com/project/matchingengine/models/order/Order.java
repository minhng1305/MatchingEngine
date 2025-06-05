package com.project.matchingengine.models.order;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.UUID;


public class Order implements Serializable{    
    private static final long serialVersionUID = 1L;

    private UUID orderId;
    private String symbol;
    private double price;
    private int originalQuantity;
    public int currentQuantity;
    private OrderSide side;
    private OrderType type;
    private double limitPrice;
    private Timestamp orderTimestamp;
    public OrderStatus status;


    public Order(UUID orderId,
                 String symbol, 
                 double price, 
                 int originalQuantity, 
                 OrderSide side, 
                 OrderType type, 
                 double limitPrice, 
                 Timestamp orderTimestamp) {
        this.orderId = orderId;
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

    public String getSymbol() {
        return symbol;
    }

    public double getPrice() {
        return price;
    }
    
    public int getOriginalQuantity() {
        return originalQuantity;
    }

    public OrderSide getSide() {
        return side;
    }

    public OrderType getType() {
        return type;
    }

    public double getLimitPrice() {
        return limitPrice;
    }

    public Timestamp getOrderTimestamp() {
        return orderTimestamp;
    }
}