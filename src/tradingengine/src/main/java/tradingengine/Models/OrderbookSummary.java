package tradingengine.models;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class OrderBookSummary {
    public List<OrderSummary> highestBuys;
    public List<OrderSummary> lowestSells;
    public String symbol;
    public Queue<Order> lastTenFulfilledOrders;

    public OrderBookSummary(List<OrderSummary> highestBuys, List<OrderSummary> lowestSells, String symbol, LinkedList<Order> lastTenFulfilledOrders) {
        this.highestBuys = highestBuys;
        this.lowestSells = lowestSells;
        this.symbol = symbol;
        this.lastTenFulfilledOrders = lastTenFulfilledOrders;
    }

    public String getSymbol() {
        return symbol;
    }   

    public static class OrderSummary {
        public long price;
        public int currentQuantity;
        public int originalQuantity;

        public OrderSummary(long price, int currentQuantity, int originalQuantity) {
            this.price = price;
            this.currentQuantity = currentQuantity;
            this.originalQuantity = originalQuantity;
        }

        public long getPrice() {
            return price;
        }

        public int getCurrentQuantity() {
            return currentQuantity;
        }

        public int getOriginalQuantity() {
            return originalQuantity;
        }
    }
}
