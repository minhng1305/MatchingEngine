package com.project.matchingengine.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

import org.springframework.stereotype.Service;

import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.models.order.OrderSide;
import com.project.matchingengine.models.order.OrderStatus;
import com.project.matchingengine.models.order.Trade;



// OrderBook class to manage the order book including order matching and trade execution
@Service
public class OrderBook {
    public String symbol;
    private PriorityQueue<Order> buyOrdersList;
    private PriorityQueue<Order> sellOrdersList;
    private ArrayList<Trade> trades;
    private double currentPrice;


    public OrderBook(String symbol) {
        this.symbol = symbol;
        this.buyOrdersList  = new PriorityQueue<>(Comparator.comparing(Order::getPrice)
                                                            .reversed()
                                                            .thenComparing(Order::getOrderTimestamp));
        this.sellOrdersList = new PriorityQueue<>(Comparator.comparing(Order::getPrice)
                                                            .thenComparing(Order::getOrderTimestamp));
        this.trades = new ArrayList<>();
        this.currentPrice = 0.0;
    }


    public void addOrder(Order order) {
        if (order.getSide() == OrderSide.BUY) {
            matchBuyOrder(order);
            if (order.currentQuantity > 0) {
                buyOrdersList.add(order);
            } else {
                processFullyFilledOrders(order);
            }
        } else {
            matchSellOrder(order);
            if (order.currentQuantity > 0) {
                sellOrdersList.add(order);
            } else {
                processFullyFilledOrders(order);
            }
        }
        if (!trades.isEmpty()) {
            this.currentPrice = trades.get(trades.size() - 1).price; // Update current price to the last trade price
        }
    }


    public void matchBuyOrder(Order buyOrder) {
        while (!sellOrdersList.isEmpty() && buyOrder.currentQuantity > 0) {
            Order sellOrder = sellOrdersList.peek();
            if (buyOrder.getPrice() >= sellOrder.getPrice()) {
                // Execute trade
                buyOrder.status = OrderStatus.PARTIALLY_FILLED;
                int tradeQuantity = Math.min(buyOrder.currentQuantity, sellOrder.currentQuantity);
                double tradePrice = sellOrder.getPrice();
                Trade trade = new Trade(this.symbol, 
                                        tradePrice, 
                                        tradeQuantity, 
                                        buyOrder.getOrderId(), 
                                        sellOrder.getOrderId(), 
                                        new Timestamp(System.currentTimeMillis()));
                trades.add(trade);

                // Update quantities
                buyOrder.currentQuantity -= tradeQuantity;
                sellOrder.currentQuantity -= tradeQuantity;

                // Remove the sell order if fully filled
                if (sellOrder.currentQuantity == 0) {
                    processFullyFilledOrders(sellOrder);
                    sellOrdersList.poll();
                }
            } else {
                break; // No more matches possible
            }
        }
    }

    public void matchSellOrder(Order sellOrder) {
        while (!buyOrdersList.isEmpty() && sellOrder.currentQuantity > 0) {
            Order buyOrder = buyOrdersList.peek();
            if (sellOrder.getPrice() <= buyOrder.getPrice()) {
                // Execute trade
                sellOrder.status = OrderStatus.PARTIALLY_FILLED;
                int tradeQuantity = Math.min(sellOrder.currentQuantity, buyOrder.currentQuantity);
                double tradePrice = sellOrder.getPrice(); //Use the sell order's price for the trade
                Trade trade = new Trade(symbol, 
                                        tradePrice, tradeQuantity, 
                                        buyOrder.getOrderId(), 
                                        sellOrder.getOrderId(), 
                                        new Timestamp(System.currentTimeMillis()));
                trades.add(trade);

                // Update quantities
                sellOrder.currentQuantity -= tradeQuantity;
                buyOrder.currentQuantity -= tradeQuantity;

                // Remove the buy order if fully filled
                if (buyOrder.currentQuantity == 0) {
                    processFullyFilledOrders(buyOrder); 
                    buyOrdersList.poll();
                }
            } else {
                break; // No more matches possible
            }
        }
    }


    public void processFullyFilledOrders(Order order) {
        System.out.println("Order fully fulfilled and removed from " + order.getSide() + " side of order book: " + order.getOrderId());     
    }


    public ArrayList<Trade> getTrades() {
        return trades;
    }


    public ArrayList<Trade> getMostRecent10Trades() {
        int start = Math.max(0, trades.size() - 10);
        ArrayList<Trade> recentTrades = new ArrayList<>(trades.subList(start, trades.size()));
        return recentTrades;
    }
    
    // public OrderBookSummery getOrderBookSummery() {

    // }


    public double getCurrentPrice() {
        if (trades == null || trades.isEmpty()) {
            return 0.0; // Or another default value like -1 or Double.NaN
        }
        this.currentPrice = trades.get(trades.size() - 1).price;
        return currentPrice;
    }

    public PriorityQueue<Order> getBuyOrdersList() {
        return buyOrdersList;
    }

    public PriorityQueue<Order> getSellOrdersList() {
        return sellOrdersList;
    }


}