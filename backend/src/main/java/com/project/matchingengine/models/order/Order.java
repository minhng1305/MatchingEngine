package com.project.matchingengine.models.order;

import java.sql.Timestamp;
import java.util.UUID;
import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;


@Entity
@Table(name = "orders")
public class Order implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private UUID orderId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private double price;

    @Column(name = "original_quantity", nullable = false)
    private int originalQuantity;

    @Column(name = "current_quantity", nullable = false)
    private int currentQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "side")
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private OrderType type;

    @Column(name = "limit_price", nullable = false)
    private double limitPrice;

    @Column(name = "order_timestamp", nullable = false)
    private Timestamp orderTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OrderStatus status;


    public Order() {
        // Default constructor for serialization/deserialization
    }
    
    public Order(UUID orderId,
                 UUID userId,
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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
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

    public int getCurrentQuantity() {
        return currentQuantity;
    }

    public void setCurrentQuantity(int currentQuantity) {
        this.currentQuantity = currentQuantity;
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

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Timestamp getOrderTimestamp() {
        return orderTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(orderId, order.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }
}
