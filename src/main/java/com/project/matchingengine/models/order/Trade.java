package com.project.matchingengine.models.order;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.UUID;

public class Trade implements Serializable {    
    private static final long serialVersionUID = 1L;
    
    public String symbol;
    public double price;
    public int quantity;
    private UUID buyOrderId;
    private UUID sellOrderId;
    public Timestamp tradeTimestamp;

    public Trade(String symbol, double price, int quantity, UUID buyOrderId, UUID sellOrderId, Timestamp tradeTimestamp) {
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.tradeTimestamp = tradeTimestamp;
    }

    public UUID getBuyOrderId() {
        return buyOrderId;
    }

    public UUID getSellOrderId() {
        return sellOrderId;
    }
}