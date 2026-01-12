package com.project.matchingengine.models.order;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class OrderBookSummary implements Serializable {
    public static final long serialVersionUID = 1L;

    public String symbol;
    public List<Order> topBuys;
    public List<Order> lowestSells;
    public double currentPrice;
    public double bestBidPrice;
    public int bestBidQuantity;
    public double bestAskPrice;
    public int bestAskQuantity;
    public List<Trade> recentTrades; // List of recent trades, e.g., last 10


    public OrderBookSummary(String symbol, 
                            List<Order> topBuys, 
                            List<Order> lowestSells, 
                            double currentPrice,
                            double bestBidPrice, 
                            int bestBidQuantity, 
                            double bestAskPrice, 
                            int bestAskQuantity,
                            List<Trade> recentTrades) {
        this.symbol = symbol;
        this.topBuys = topBuys;
        this.lowestSells = lowestSells;
        this.currentPrice = currentPrice;
        this.bestBidPrice = bestBidPrice;
        this.bestBidQuantity = bestBidQuantity;
        this.bestAskPrice = bestAskPrice;
        this.bestAskQuantity = bestAskQuantity;
        this.recentTrades = recentTrades;
    }
    
    public void updateOrderBookSummary(List<Order> topBuys,
                                       List<Order> lowestSells,
                                       double currentPrice,
                                       double bestBidPrice,
                                       int bestBidQuantity,
                                       double bestAskPrice,
                                       int bestAskQuantity,
                                       List<Trade> recentTrades) {
        this.topBuys = topBuys;
        this.lowestSells = lowestSells;
        this.currentPrice = currentPrice;
        this.bestBidPrice = bestBidPrice;
        this.bestBidQuantity = bestBidQuantity;
        this.bestAskPrice = bestAskPrice;
        this.bestAskQuantity = bestAskQuantity;
        this.recentTrades = recentTrades;
    }
}

