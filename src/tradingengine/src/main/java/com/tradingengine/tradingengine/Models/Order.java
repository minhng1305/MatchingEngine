package com.tradingengine.tradingengine.Models;

public class Order {

    public String OrderId;
    public String Username;
    public String OrderType;
    public long Price;
    public int CurrentQuantity;
    public int OriginalQuantity;

    public Order() {
        this.OriginalQuantity = 0;
    }
    
    public Order(String orderId, String username, String isBuySide, long price, int quantity) {
        this.OrderId = orderId;
        this.Username = username;
        this.OrderType = isBuySide;
        this.Price = price;
        this.CurrentQuantity= quantity;
        this.OriginalQuantity = quantity;
    }

    public String getOrderId() {
        return this.OrderId;
    }

    public long getPrice() {
        return this.Price;
    }

    public int getCurrentQuantity() {
        return this.CurrentQuantity;
    }

    public int getOriginalQuantity() {
        return this.OriginalQuantity;
    }

    public void IncreaseQuantity(int quantityDelta) {
        this.CurrentQuantity += quantityDelta;
    }

    public void DecreaseQuantity(int quantityDelta) {
        if (quantityDelta > this.CurrentQuantity) {
            throw new IllegalArgumentException("Cannot decrease quantity by more than current quantity for orderId = " + this.OrderId);
        }
        this.CurrentQuantity -= quantityDelta;
    }

    @Override
    public String toString() {
        return "OrderRequest{"
            + "type="
            + OrderType
            + ", notionalAmount="
            + CurrentQuantity
            + ", originalNotionalAmount="
            + OriginalQuantity
            + ", price="
            + Price
            + '}';
    }


}
