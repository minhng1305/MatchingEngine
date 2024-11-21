package tradingengine.services;

import java.util.PriorityQueue;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;

import tradingengine.models.Order;
import tradingengine.models.OrderBookSummary;
import tradingengine.models.Trade;

@Service
public class OrderBook {
    public String symbol;

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    // Buy orders are sorted descendingly 
    public PriorityQueue<Order> buyOrdersList = new PriorityQueue<>( Comparator.comparingLong(Order::getPrice).reversed()
                                                                                .thenComparing(Order::getTimestamp) );
    // Sell orders are sorted ascendingly
    public PriorityQueue<Order> sellOrdersList = new PriorityQueue<>( Comparator.comparingLong(Order::getPrice)
                                                                                .thenComparing(Order::getTimestamp) );
    public List<Trade> Trades = new ArrayList<>();

    public LinkedList<Order> lastTenFulfilledOrders = new LinkedList<>();

    public List<Trade> addOrder(Order order) {
        List<Trade> trades = new ArrayList<>();
        if (order.side == Order.Side.BUY.toString()) {
            Trades.addAll(matchBuyOrders(order));
        } else if (order.side == Order.Side.SELL.toString()) {
            Trades.addAll(matchSellOrders(order));
        } else {
            throw new IllegalArgumentException("Invalid order side");
        }
        return trades;
    }

    public List<Trade> matchBuyOrders(Order buyOrder) {
        List<Trade> trades = new ArrayList<>();
        buyOrdersList.add(buyOrder);
        while (!sellOrdersList.isEmpty() && sellOrdersList.peek().getPrice() <= buyOrder.getPrice() && buyOrder.currentQuantity > 0) {
            Order sellOrder = sellOrdersList.poll();
            trades.add(executeTrade(buyOrder, sellOrder));
            if (sellOrder.currentQuantity > 0) {
                sellOrdersList.offer(sellOrder);
            } else {
                processFulfilledOrder(sellOrder);
            }
            if (buyOrder.currentQuantity == 0) {
                processFulfilledOrder(buyOrder);
                break;
            }
        }
        if (buyOrder.currentQuantity > 0) {
            buyOrdersList.offer(buyOrder);
        }
        return trades;
    }

    public List<Trade> matchSellOrders(Order sellOrder) {
        List<Trade> trades = new ArrayList<>();
        sellOrdersList.add(sellOrder);
        while (!buyOrdersList.isEmpty() && buyOrdersList.peek().getPrice() >= sellOrder.getPrice() && sellOrder.currentQuantity > 0) {
            Order buyOrder = buyOrdersList.poll();
            trades.add(executeTrade(buyOrder, sellOrder));
            if (buyOrder.currentQuantity > 0) {
                buyOrdersList.offer(buyOrder);
            } else {
                processFulfilledOrder(buyOrder);
            }
            if (sellOrder.currentQuantity == 0) {
                processFulfilledOrder(sellOrder);
                break;
            }
        }
        if (sellOrder.currentQuantity > 0) {
            sellOrdersList.offer(sellOrder);
        }
        return trades;
    }

    public Trade executeTrade(Order buyOrder, Order sellOrder) {
        int tradeQuantity = Math.min(buyOrder.currentQuantity, sellOrder.currentQuantity);
        long tradePrice = sellOrder.getPrice();
        buyOrder.currentQuantity -= tradeQuantity;
        sellOrder.currentQuantity -= tradeQuantity;
        return new Trade(
                        buyOrder.getOrderId(), 
                        sellOrder.getOrderId(), 
                        tradePrice, tradeQuantity, 
                        buyOrder.getOriginalQuantity(), 
                        sellOrder.getOriginalQuantity());
    }

    public void processFulfilledOrder(Order order) {
        System.out.println("Order fulfilled: " + order.getOrderId());
        lastTenFulfilledOrders.add(order);
        if (lastTenFulfilledOrders.size() > 10) {
            lastTenFulfilledOrders.poll();
        }
    }

    public OrderBookSummary getOrderBookSummary() {
        List<OrderBookSummary.OrderSummary> highestBuys = new ArrayList<>();
        List<OrderBookSummary.OrderSummary> lowestSells = new ArrayList<>();
        // Get top 5 highest buy orders
        PriorityQueue<Order> buyOrdersListCopy = new PriorityQueue<>(buyOrdersList);
        for (int i = 0; i < 5 && !buyOrdersListCopy.isEmpty(); i++) {
            Order order = buyOrdersListCopy.poll();
            highestBuys.add(new OrderBookSummary.OrderSummary(order.getPrice(), order.getCurrentQuantity(), order.getOriginalQuantity()));
        }
        // Get top 5 lowest sell orders
        PriorityQueue<Order> sellOrdersListCopy = new PriorityQueue<>(sellOrdersList);
        for (int i = 0; i < 5 && !sellOrdersListCopy.isEmpty(); i++) {
            Order order = sellOrdersListCopy.poll();
            lowestSells.add(new OrderBookSummary.OrderSummary(order.getPrice(), order.getCurrentQuantity(), order.getOriginalQuantity()));
        }
        return new OrderBookSummary(highestBuys, lowestSells, symbol, lastTenFulfilledOrders);
    }
}
