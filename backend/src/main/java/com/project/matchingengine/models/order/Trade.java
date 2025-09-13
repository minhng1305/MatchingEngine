package com.project.matchingengine.models.order;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Table(name = "trades")
public class Trade implements Serializable {    
    private static final long serialVersionUID = 1L;

    @Id
    private UUID tradeId;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "buyOrderId", nullable = false)
    private UUID buyOrderId;

    @Column(name = "sellOrderId", nullable = false)
    private UUID sellOrderId;

    @Column(name = "trade_timestamp", nullable = false)
    private Timestamp tradeTimestamp;

    public Trade() {
        // Default constructor for serialization/deserialization
    }

    public Trade(UUID tradeId,
                 String symbol,
                 double price,
                 int quantity,
                 UUID buyOrderId,
                 UUID sellOrderId,
                 Timestamp tradeTimestamp) {
        this.tradeId = tradeId;
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.tradeTimestamp = tradeTimestamp;
    }

    public UUID getTradeId() {
        return tradeId;
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public UUID getBuyOrderId() {
        return buyOrderId;
    }

    public UUID getSellOrderId() {
        return sellOrderId;
    }

    public Timestamp getTradeTimestamp() {
        return tradeTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Trade trade = (Trade) o;
        return Objects.equals(tradeId, trade.tradeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tradeId);
    }
}