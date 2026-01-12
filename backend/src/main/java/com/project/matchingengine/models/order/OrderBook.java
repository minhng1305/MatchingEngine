package com.project.matchingengine.models.order;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.UUID;

import com.project.matchingengine.service.authentication.UserDetailsCacheService;


public class OrderBook {
    private final String symbol;
    private final PriorityQueue<Order> buyOrdersList;
    private final PriorityQueue<Order> sellOrdersList;
    private final ArrayList<Trade> trades;
    private double currentPrice;
    private final Queue<Order> lastTenFulfilledOrders;
    private final UserDetailsCacheService userDetailsCacheService;
    private final OrderBookSummary orderBookSummary;

    public OrderBook(String symbol,
                     UserDetailsCacheService userDetailsCacheService)
    {
        this.symbol = symbol;
        this.buyOrdersList  = new PriorityQueue<>(Comparator.comparing(Order::getPrice)
                                                            .reversed()
                                                            .thenComparing(Order::getOrderTimestamp));
        this.sellOrdersList = new PriorityQueue<>(Comparator.comparing(Order::getPrice)
                                                            .thenComparing(Order::getOrderTimestamp));
        this.trades = new ArrayList<>();
        this.currentPrice = 0.0;
        this.lastTenFulfilledOrders = new LinkedList<>();
        this.userDetailsCacheService = userDetailsCacheService;
        this.orderBookSummary = new OrderBookSummary(symbol, new ArrayList<>(), new ArrayList<>(), currentPrice, 0.0, 0, 0.0, 0, new ArrayList<>());

    }

    public String getSymbol() {
        return symbol;
    }

    public void addOrder(Order order) {
        if (order.getSide() == OrderSide.BUY) {
            matchBuyOrder(order);
            if (order.getCurrentQuantity() > 0) {
                buyOrdersList.add(order);
            } else {
                processFullyFilledOrders(order);
            }
        } else {
            matchSellOrder(order);
            if (order.getCurrentQuantity() > 0) {
                sellOrdersList.add(order);
            } else {
                processFullyFilledOrders(order);
            }
        }
        if (!trades.isEmpty()) {
            this.currentPrice = trades.get(trades.size() - 1).getPrice();
        }
    }

    public void matchBuyOrder(Order buyOrder) {
        while (!sellOrdersList.isEmpty() && buyOrder.getCurrentQuantity() > 0) {
            Order sellOrder = sellOrdersList.peek();
            if (buyOrder.getUserId() != sellOrder.getUserId() && buyOrder.getPrice() >= sellOrder.getPrice()) {
                int tradeQuantity = Math.min(buyOrder.getCurrentQuantity(), sellOrder.getCurrentQuantity());
                double tradePrice = getTradePrice(buyOrder, sellOrder);
                Trade trade = new Trade(UUID.randomUUID(),
                                        this.symbol,
                                        tradePrice, 
                                        tradeQuantity, 
                                        buyOrder.getOrderId(), 
                                        sellOrder.getOrderId(),
                                        buyOrder.getUserId(),
                                        sellOrder.getUserId(),
                                        new Timestamp(System.currentTimeMillis()));
                trades.add(trade);

                // Update capital and ESG points for both users via caching
                userDetailsCacheService.applyTrade(buyOrder.getUserId(), symbol, tradeQuantity, tradePrice, buyOrder.getPrice(), true);
                userDetailsCacheService.applyTrade(sellOrder.getUserId(), symbol, tradeQuantity, tradePrice, sellOrder.getPrice(), false);

                // Update order statuses and quantities
                buyOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
                if (sellOrder.getStatus() != OrderStatus.PARTIALLY_FILLED)
                    sellOrder.setStatus(OrderStatus.PARTIALLY_FILLED);

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
            if (buyOrder.getUserId() != sellOrder.getUserId() && sellOrder.getPrice() <= buyOrder.getPrice()) {
                int tradeQuantity = Math.min(sellOrder.getCurrentQuantity(), buyOrder.getCurrentQuantity());
                double tradePrice = getTradePrice(buyOrder, sellOrder);
                Trade trade = new Trade(UUID.randomUUID(),
                                        this.symbol,
                                        tradePrice, tradeQuantity, 
                                        buyOrder.getOrderId(), 
                                        sellOrder.getOrderId(),
                                        buyOrder.getUserId(),
                                        sellOrder.getUserId(),
                                        new Timestamp(System.currentTimeMillis()));
                trades.add(trade);

                // Update capital and ESG points for both users
                userDetailsCacheService.applyTrade(buyOrder.getUserId(), symbol, tradeQuantity, tradePrice, buyOrder.getPrice(), true);
                userDetailsCacheService.applyTrade(sellOrder.getUserId(), symbol, tradeQuantity, tradePrice, sellOrder.getPrice(), false);

                // Update order statuses and quantities
                sellOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
                if (buyOrder.getStatus() != OrderStatus.PARTIALLY_FILLED)
                    buyOrder.setStatus(OrderStatus.PARTIALLY_FILLED);

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

    /*
     * This is to determine the trade price based on both the market maker and market taker orders.
     * */
    public double getTradePrice(Order buyOrder, Order sellOrder) {
        double tradePrice = 0.0;

        if ( buyOrder.getStatus().equals(OrderStatus.PENDING) && sellOrder.getStatus().equals(OrderStatus.PARTIALLY_FILLED) ) {
            tradePrice = sellOrder.getPrice();
        } else if ( buyOrder.getStatus().equals(OrderStatus.PARTIALLY_FILLED) && sellOrder.getStatus().equals(OrderStatus.PENDING) ) {
            tradePrice = buyOrder.getPrice();
        } else if ( ( buyOrder.getStatus().equals(OrderStatus.PARTIALLY_FILLED) && sellOrder.getStatus().equals(OrderStatus.PARTIALLY_FILLED) ) ||
                ( buyOrder.getStatus().equals(OrderStatus.PENDING) && sellOrder.getStatus().equals(OrderStatus.PENDING) ) )
        {
            Timestamp buyTimestamp = buyOrder.getOrderTimestamp();
            Timestamp sellTimestamp = sellOrder.getOrderTimestamp();
            if (buyTimestamp.before(sellTimestamp)) {
                tradePrice = buyOrder.getPrice();
            } else {
                tradePrice = sellOrder.getPrice();
            }
        }
        return tradePrice;
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

    public OrderBookSummary getOrderBookSummary() {
        return this.orderBookSummary;
    }

    public void updateOrderBookSummary() {
        List<Order> topBuys = new ArrayList<>();
        List<Order> lowestSells = new ArrayList<>();

        // Get top 5 buy orders
        PriorityQueue<Order> tempBuyOrders = new PriorityQueue<>(buyOrdersList);
        for (int i = 0; i < 5 && !tempBuyOrders.isEmpty(); i++) {
            topBuys.add(tempBuyOrders.poll());
        } 
        // Get bottom 5 sell orders
        PriorityQueue<Order> tempSellOrders = new PriorityQueue<>(sellOrdersList);
        for (int i = 0; i < 5 && !tempSellOrders.isEmpty(); i++) {
            lowestSells.add(tempSellOrders.poll());
        }
        
        // Safely get best bid (buy) price and quantity, defaulting to 0 if queue is empty
        double bestBidPrice = 0.0;
        int bestBidQuantity = 0;
        if (!buyOrdersList.isEmpty()) {
            Order bestBid = buyOrdersList.peek();
            if (bestBid != null) {
                bestBidPrice = bestBid.getPrice();
                bestBidQuantity = bestBid.getCurrentQuantity();
            }
        }
        
        // Safely get best ask (sell) price and quantity, defaulting to 0 if queue is empty
        double bestAskPrice = 0.0;
        int bestAskQuantity = 0;
        if (!sellOrdersList.isEmpty()) {
            Order bestAsk = sellOrdersList.peek();
            if (bestAsk != null) {
                bestAskPrice = bestAsk.getPrice();
                bestAskQuantity = bestAsk.getCurrentQuantity();
            }
        }
        
        orderBookSummary.updateOrderBookSummary(
                topBuys,
                lowestSells,
                currentPrice,
                bestBidPrice,
                bestBidQuantity,
                bestAskPrice,
                bestAskQuantity,
                getMostRecent10Trades());
    }

    public void clearTradeRecords() {
        this.trades.clear();
    }

    /**
     * Get all orders that need to be persisted to the database.
     * This includes active orders (in queues) and fully filled orders.
     */
    public List<Order> getAllOrdersToUpdate() {
        List<Order> ordersToUpdate = new ArrayList<>();
        
        // Add all active buy orders
        ordersToUpdate.addAll(buyOrdersList);
        
        // Add all active sell orders
        ordersToUpdate.addAll(sellOrdersList);
        
        // Add all fully filled orders
        ordersToUpdate.addAll(lastTenFulfilledOrders);
        
        return ordersToUpdate;
    }
}