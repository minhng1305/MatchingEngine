package com.project.matchingengine.service.orderbook;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.models.order.OrderBookSummary;
import com.project.matchingengine.models.order.OrderSide;
import com.project.matchingengine.models.order.OrderStatus;
import com.project.matchingengine.models.order.Trade;


// @Service
public class OrderBook {
    public String symbol;
    private final PriorityQueue<Order> buyOrdersList;
    private final PriorityQueue<Order> sellOrdersList;
    private final ArrayList<Trade> trades;
    private double currentPrice;
    private final Queue<Order> lastTenFulfilledOrders;


    public OrderBook(String symbol) {
        this.symbol = symbol;
        this.buyOrdersList  = new PriorityQueue<>(Comparator.comparing(Order::getPrice)
                                                            .reversed()
                                                            .thenComparing(Order::getOrderTimestamp));
        this.sellOrdersList = new PriorityQueue<>(Comparator.comparing(Order::getPrice)
                                                            .thenComparing(Order::getOrderTimestamp));
        this.trades = new ArrayList<>();
        this.currentPrice = 0.0;
        this.lastTenFulfilledOrders = new LinkedList<>();
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
            this.currentPrice = trades.get(trades.size() - 1).price;
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

                sellOrder.currentQuantity -= tradeQuantity;
                buyOrder.currentQuantity -= tradeQuantity;

                if (buyOrder.currentQuantity == 0) {
                    processFullyFilledOrders(buyOrder); 
                    buyOrdersList.poll();
                }
            } else {
                break;
            }
        }
    }


    public void processFullyFilledOrders(Order order) {
        System.out.println(symbol + " - Order fully fulfilled and removed from " + order.getSide() + " side of order book: " + order.getOrderId());
        order.status = OrderStatus.FILLED;

        lastTenFulfilledOrders.offer(order);
        if (lastTenFulfilledOrders.size() >= 10) {
            lastTenFulfilledOrders.poll();
        }
    }


    public ArrayList<Trade> getTrades() {
        return trades;
    }


    public ArrayList<Trade> getMostRecent10Trades() {
        int start = Math.max(0, trades.size() - 10);
        return new ArrayList<>(trades.subList(start, trades.size()));
    }


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


    public OrderBookSummary getOrderBookSummary() {
        List<Order> topBuys = new ArrayList<>();
        List<Order> lowestSells = new ArrayList<>();

        // Get top 5 buy orders
        PriorityQueue<Order> tempBuyOrders = new PriorityQueue<>(buyOrdersList);
        for (int i = 0; i < 5 && !tempBuyOrders.isEmpty(); i++) {
            topBuys.add(tempBuyOrders.poll());

        } 
        // Get lowest 5 sell orders 
        PriorityQueue<Order> tempSellOrders = new PriorityQueue<>(sellOrdersList);
        for (int i = 0; i < 5 && !tempSellOrders.isEmpty(); i++) {
            lowestSells.add(tempSellOrders.poll());
        }
        return new OrderBookSummary(
            this.symbol,
            topBuys,
            lowestSells,
            getCurrentPrice(),
            buyOrdersList.isEmpty() ? 0.0 : buyOrdersList.peek().getPrice(),
            buyOrdersList.isEmpty() ? 0 : buyOrdersList.peek().getOriginalQuantity(),
            sellOrdersList.isEmpty() ? 0.0 : sellOrdersList.peek().getPrice(),
            sellOrdersList.isEmpty() ? 0 : sellOrdersList.peek().getOriginalQuantity(),
            getMostRecent10Trades()
        );
    }
}