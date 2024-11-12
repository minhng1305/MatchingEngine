package tradingengine.Models;

import java.util.*;;

public class OrderbookSummary {
    
    public List<OrderSummary> TopBuys;
    public List<OrderSummary> LowestSells;
    public Queue<Order> LastTenFulfilledOrders;
    // public String Symbol;

    public OrderbookSummary(List<OrderSummary> topBuys, List<OrderSummary> lowestSells,  Queue<Order> lastTenFulfilledOrders) {
        this.TopBuys = topBuys;
        this.LowestSells = lowestSells;
        this.LastTenFulfilledOrders = lastTenFulfilledOrders;
        // this.Symbol = symbol;
    }

    // public String getSymbol() {
    //     return Symbol;
    // }

    public static class OrderSummary {
        public long Price;
        public int CurrentQuantity;
        public int OriginalQuantity;

        public OrderSummary(long price, int currentQuantity, int originalQuantity) {
            this.Price = price;
            this.CurrentQuantity = currentQuantity;
            this.OriginalQuantity = originalQuantity;
        }
    }


}
