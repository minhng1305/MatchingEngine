package com.project.matchingengine.models.order;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

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

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReadLock readLock = lock.readLock();
    private final WriteLock writeLock = lock.writeLock();

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
        writeLock.lock();
        try {
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
        } finally {
            writeLock.unlock();
        }
    }

    // TODO: Orders from same user should not match at all. Yet Error continues to exist
    private void matchBuyOrder(Order buyOrder) {
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

    // TODO: Orders from same user should not match at all. Yet Error continues to exist
    private void matchSellOrder(Order sellOrder) {
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


    private void processFullyFilledOrders(Order order) {
        order.setStatus(OrderStatus.FILLED);

        lastTenFulfilledOrders.offer(order);
        if (lastTenFulfilledOrders.size() >= 10) {
            lastTenFulfilledOrders.poll();
        }
    }

    public ArrayList<Trade> getTrades() {
        readLock.lock();
        try {
            return new ArrayList<>(trades); // Return copy to prevent external modification
        } finally {
            readLock.unlock();
        }
    }

    public ArrayList<Trade> getMostRecent10Trades() {
        readLock.lock();
        try {
            int start = Math.max(0, trades.size() - 10);
            return new ArrayList<>(trades.subList(start, trades.size()));
        } finally {
            readLock.unlock();
        }
    }

    public double getCurrentPrice() {
        readLock.lock();
        try {
            if (trades == null || trades.isEmpty()) {
                return 0.0; // Or another default value like -1 or Double.NaN
            }
            // Update currentPrice from last trade
            this.currentPrice = trades.get(trades.size() - 1).getPrice();
            return currentPrice;
        } finally {
            readLock.unlock();
        }
    }

    public OrderBookSummary getOrderBookSummary() {
        readLock.lock();
        try {
            return this.orderBookSummary;
        } finally {
            readLock.unlock();
        }
    }

    public void updateOrderBookSummary() {
        readLock.lock();
        try {
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
        } finally {
            readLock.unlock();
        }
    }


    public void clearTradeRecords() {
        writeLock.lock();
        try {
            this.trades.clear();
        } finally {
            writeLock.unlock();
        }
    }


    public List<Order> getAllOrdersToUpdate() {
        readLock.lock();
        try {
            List<Order> ordersToUpdate = new ArrayList<>();
            ordersToUpdate.addAll(new ArrayList<>(buyOrdersList));
            ordersToUpdate.addAll(new ArrayList<>(sellOrdersList));
            ordersToUpdate.addAll(new ArrayList<>(lastTenFulfilledOrders));
            return ordersToUpdate;
        } finally {
            readLock.unlock();
        }
    }
}