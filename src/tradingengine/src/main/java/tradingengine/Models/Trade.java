package tradingengine.Models;

public class Trade {
    String BuyOrderId;
    String SellOrderId;
    long Price;
    int Quantity;
    int OriginalBuyQuantity;
    int OriginalSellQuantity;

    public Trade(String buyOrderId, String sellOrderId, long price, int quantity, int originalBuyQuantity, int originalSellQuantity) {
        this.BuyOrderId = buyOrderId;
        this.SellOrderId = sellOrderId;
        this.Price = price;
        this.Quantity = quantity;
        this.OriginalBuyQuantity = originalBuyQuantity;
        this.OriginalSellQuantity = originalSellQuantity;
    }
}
