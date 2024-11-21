package tradingengine.models;

import java.sql.Timestamp;
import java.util.UUID;

// Trading records
public class Trade {
    private UUID buyOrderId;
    private UUID sellOrderId;
    private long price;
    public int originalBuyQuantity;
    public int originalSellQuantity;
    public int tradingQuantity;
    public Timestamp timestamp;

    public Trade(UUID buyOrderId, UUID sellOrderId, long price, int tradingQuantity, int originalBuyQuantity, int originalSellQuantity) {
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.price = price;
        this.tradingQuantity = tradingQuantity;
        this.originalBuyQuantity = originalBuyQuantity;
        this.originalSellQuantity = originalSellQuantity;
    }

    public UUID getBuyOrderId() {
        return buyOrderId;
    }

    public UUID getSellOrderId() {
        return sellOrderId;
    }

    public long getPrice() {
        return price;
    }

    public int getTradingQuantity() {
        return tradingQuantity;
    }
}
