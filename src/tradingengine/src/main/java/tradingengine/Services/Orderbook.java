package tradingengine.Services;
import java.util.*;

import org.springframework.stereotype.Service;

import tradingengine.Models.Order;
import tradingengine.Models.Trade;
import tradingengine.Models.OrderSide;
import tradingengine.Models.OrderbookSummary;

@Service
public class Orderbook {
    // public String Symbol;

    // public void SetSymbol(String symbol) {
    //     this.Symbol = symbol;
    // }

    // MAX Heap for Buy Orders -> Sorted Descendingly
    private final PriorityQueue<Order> BuyOrdersList = new PriorityQueue<>(Comparator.comparing(Order::getPrice).reversed().thenComparing(Order::getCurrentQuantity));
    // MIN Heap for Sell Orders -> Sorted Ascendingly
    private final PriorityQueue<Order> SellOrdersList = new PriorityQueue<>(Comparator.comparing(Order::getPrice).thenComparing(Order::getCurrentQuantity));

    private final Queue<Order> lastTenFulfilledOrders = new LinkedList<>();

    public List<Trade> InsertOrder(Order order) {
        List<Trade> trades = new ArrayList<>();
        if (order.OrderType.equals(OrderSide.BID.toString())) {
            trades.addAll(MatchBuyOrders(order));
        } else if (order.OrderType.equals(OrderSide.ASK.toString())) {
            trades.addAll(MatchSellOrders(order));
        } else {
            throw new IllegalArgumentException("Unknown order type: " + order.OrderType);
        }
        return trades;
    }

    private List<Trade> MatchBuyOrders(Order buyOrder) {
        List<Trade> tradesRecord = new ArrayList<>();
        while (this.SellOrdersList.isEmpty() && buyOrder.CurrentQuantity > 0 && buyOrder.Price >= this.SellOrdersList.peek().Price) {
            Order sellOrder = this.SellOrdersList.poll();
            tradesRecord.add(ExecuteTrade(buyOrder, sellOrder));

            if (sellOrder.CurrentQuantity > 0) {
                this.SellOrdersList.add(sellOrder);
            } else {
                ProcessFullyFulfilledOrder(sellOrder);
            }
            if (buyOrder.CurrentQuantity == 0) {
                ProcessFullyFulfilledOrder(buyOrder);
                break;
            }
        }
        if (buyOrder.CurrentQuantity > 0) 
            this.BuyOrdersList.offer(buyOrder);
        return tradesRecord;
    }

    private List<Trade> MatchSellOrders(Order sellOrder) {
        List<Trade> tradesRecord = new ArrayList<>();
        while (this.BuyOrdersList.isEmpty() && sellOrder.CurrentQuantity > 0 && sellOrder.Price <= this.BuyOrdersList.peek().Price) {
            Order buyOrder = this.BuyOrdersList.poll();
            tradesRecord.add(ExecuteTrade(buyOrder, sellOrder));

            if (buyOrder.CurrentQuantity > 0) {
                this.BuyOrdersList.add(buyOrder);
            } else {
                ProcessFullyFulfilledOrder(buyOrder);
            }
            if (sellOrder.CurrentQuantity == 0) {
                ProcessFullyFulfilledOrder(sellOrder);
                break;
            }
        }
        if (sellOrder.CurrentQuantity > 0) 
            this.SellOrdersList.offer(sellOrder);
        return tradesRecord;
    }

    private Trade ExecuteTrade(Order buyOrder, Order sellOrder) {
        // Assuming Price-time priority
        long tradePrice = sellOrder.Price;
        int tradeQuantity = Math.min(buyOrder.CurrentQuantity, sellOrder.CurrentQuantity);
        buyOrder.DecreaseQuantity(tradeQuantity);
        sellOrder.DecreaseQuantity(tradeQuantity);

        return new Trade(buyOrder.OrderId, sellOrder.OrderId, tradePrice, tradeQuantity, buyOrder.getOriginalQuantity(), sellOrder.getOriginalQuantity());
    }
    
    private void ProcessFullyFulfilledOrder(Order order) {
        // Log the order as fully fulfilled
        System.out.println("Order fully fulfilled and removed from order book: " + order.OrderId);
        lastTenFulfilledOrders.offer(order);
        // If the queue size exceeds 10, remove the oldest element
        if (lastTenFulfilledOrders.size() > 10) 
            lastTenFulfilledOrders.poll();
    }

    public OrderbookSummary GetOrderbookSummary() {
        List<OrderbookSummary.OrderSummary> topBuys = new ArrayList<>();
        List<OrderbookSummary.OrderSummary> lowestSells = new ArrayList<>();
        // Get top 5 buy orders
        PriorityQueue<Order> tempBuyOrders = new PriorityQueue<>(this.BuyOrdersList);
        for (int i = 0; i < 5 && !tempBuyOrders.isEmpty(); i++) {
            Order order = tempBuyOrders.poll();
            topBuys.add(new OrderbookSummary.OrderSummary(order.Price, order.CurrentQuantity, order.OriginalQuantity));
        }
        // Get lowest 5 sell orders
        PriorityQueue<Order> tempSellOrders = new PriorityQueue<>(this.SellOrdersList);
        for (int i = 0; i < 5 && !tempSellOrders.isEmpty(); i++) {
            Order order = tempSellOrders.poll();
            lowestSells.add(new OrderbookSummary.OrderSummary(order.Price, order.CurrentQuantity, order.OriginalQuantity));
        }
        return new OrderbookSummary(topBuys, lowestSells, lastTenFulfilledOrders);
    }
}
