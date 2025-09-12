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
    private String symbol;
    private final PriorityQueue<Order> buyOrdersList;
    private final PriorityQueue<Order> sellOrdersList;
    private final ArrayList<Trade> trades;
    private double currentPrice;
    private final Queue<Order> lastTenFulfilledOrders;
    private OrderService orderService;


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
            if (order.getCurrentQuantity() > 0) {
                buyOrdersList.add(order);
                orderService.updateOrder(order);
            } else {
                processFullyFilledOrders(order);
            }
        } else {
            matchSellOrder(order);
            if (order.getCurrentQuantity() > 0) {
                sellOrdersList.add(order);
                orderService.updateOrder(order);
            } else {
                processFullyFilledOrders(order);
                orderService.removeOrder(order.getOrderId());
            }
        }
        if (!trades.isEmpty()) {
            this.currentPrice = trades.get(trades.size() - 1).getPrice();
        }
    }


    public void matchBuyOrder(Order buyOrder) {
        while (!sellOrdersList.isEmpty() && buyOrder.getCurrentQuantity() > 0) {
            Order sellOrder = sellOrdersList.peek();
            if (buyOrder.getPrice() >= sellOrder.getPrice()) {

                buyOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
                int tradeQuantity = Math.min(buyOrder.getCurrentQuantity(), sellOrder.getCurrentQuantity());
                double tradePrice = sellOrder.getPrice();
                Trade trade = new Trade(this.symbol, 
                                        tradePrice, 
                                        tradeQuantity, 
                                        buyOrder.getOrderId(), 
                                        sellOrder.getOrderId(), 
                                        new Timestamp(System.currentTimeMillis()));
                trades.add(trade);

                buyOrder.setCurrentQuantity(buyOrder.getCurrentQuantity() - tradeQuantity);
                sellOrder.setCurrentQuantity(sellOrder.getCurrentQuantity() - tradeQuantity);

                if (sellOrder.getCurrentQuantity() == 0) {
                    processFullyFilledOrders(sellOrder);
                    sellOrdersList.poll();
                }
            } else {
                break;
            }
        }
    }

    
    public void matchSellOrder(Order sellOrder) {
        while (!buyOrdersList.isEmpty() && sellOrder.getCurrentQuantity() > 0) {
            Order buyOrder = buyOrdersList.peek();
            if (sellOrder.getPrice() <= buyOrder.getPrice()) {
                // Execute trade
                sellOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
                int tradeQuantity = Math.min(sellOrder.getCurrentQuantity(), buyOrder.getCurrentQuantity());
                double tradePrice = sellOrder.getPrice(); //Use the sell order's price for the trade
                Trade trade = new Trade(symbol, 
                                        tradePrice, tradeQuantity, 
                                        buyOrder.getOrderId(), 
                                        sellOrder.getOrderId(), 
                                        new Timestamp(System.currentTimeMillis()));
                trades.add(trade);

                sellOrder.setCurrentQuantity(sellOrder.getCurrentQuantity() - tradeQuantity);
                buyOrder.setCurrentQuantity(buyOrder.getCurrentQuantity() - tradeQuantity);

                if (buyOrder.getCurrentQuantity() == 0) {
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
        order.setStatus(OrderStatus.FILLED);

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
        this.currentPrice = trades.get(trades.size() - 1).getPrice();
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