package com.project.matchingengine.tests;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.UUID;

import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.models.order.OrderSide;
import com.project.matchingengine.models.order.OrderType;
import com.project.matchingengine.models.order.Trade;
import com.project.matchingengine.service.orderbook.OrderBook;

public class OrderBookVerification {

    public static void main(String[] args) {

        long startTime = System.currentTimeMillis();

        // Create an order book
        OrderBook orderBook = new OrderBook("AAPL");
        System.out.println("Created OrderBook for symbol: " + orderBook.symbol);
        
        // Create some test orders
        Order buyOrder1 = createOrder("AAPL", 150.0, 100, OrderSide.BUY);
        Order sellOrder1 = createOrder("AAPL", 148.0, 50, OrderSide.SELL);
        Order buyOrder2 = createOrder("AAPL", 149.5, 200, OrderSide.BUY);
        Order sellOrder2 = createOrder("AAPL", 149.0, 150, OrderSide.SELL);

        
        // Add orders to the book
        System.out.println("\nAdding buy order: " + buyOrder1.getOrderId() + " || price: " + buyOrder1.getPrice() + " || quantity: " + buyOrder1.getOriginalQuantity());
        orderBook.addOrder(buyOrder1);
        
        System.out.println("\nAdding sell order: " + sellOrder1.getOrderId() + " || price: " + sellOrder1.getPrice() + " || quantity: " + sellOrder1.getOriginalQuantity());
        orderBook.addOrder(sellOrder1);
        
        // Check trades
        System.out.println("\nTrades after first two orders:");
        printTrades(orderBook.getTrades());
        
        // Add more orders
        System.out.println("\nAdding buy order: " + buyOrder2.getOrderId() + " || price: " + buyOrder2.getPrice() + " || quantity: " + buyOrder2.getOriginalQuantity());
        orderBook.addOrder(buyOrder2);
        
        System.out.println("\nAdding sell order: " + sellOrder2.getOrderId() + " || price: " + sellOrder2.getPrice() + " || quantity: " + sellOrder2.getOriginalQuantity());
        orderBook.addOrder(sellOrder2);
        
        // Check final trades
        System.out.println("\nFinal trades:");
        printTrades(orderBook.getTrades());
        
        System.out.println("\nMost recent 10 trades:");
        printTrades(orderBook.getMostRecent10Trades());

        // Current stock price
        System.out.println("\nCurrent stock price: " + orderBook.getCurrentPrice());


        long endTime = System.currentTimeMillis();

        System.out.println("\nOrderBook verification completed in " + (endTime - startTime) + " ms");
    }
    
    private static Order createOrder(String symbol, double price, int quantity, OrderSide side) {
        return new Order(
            UUID.randomUUID(),
            symbol,
            price,
            quantity,
            side,
            OrderType.LIMIT,
            price,
            new Timestamp(System.currentTimeMillis())
        );
    }
    
    private static void printTrades(ArrayList<Trade> trades) {
        if (trades.isEmpty()) {
            System.out.println("No trades executed");
            return;
        }
        
        for (int i = 0; i < trades.size(); i++) {
            Trade trade = trades.get(i);
            System.out.println("Trade " + (i + 1) + ": " + trade.symbol + 
                               " || Price: " + trade.price + 
                               " || Quantity: " + trade.quantity + 
                               " || Buy Order: " + trade.getBuyOrderId() + 
                               " || Sell Order: " + trade.getSellOrderId() +
                               " || Timestamp: " + trade.tradeTimestamp);
        }
    }

    // private static void testOrderBookSerialization(OrderBook orderBook) {
    //     // Prevent instantiation
    //     try {
    //         // Try to serialize it to a byte array
    //         System.out.println("baos");
    //         ByteArrayOutputStream baos = new ByteArrayOutputStream();
    //         System.out.println("oos");
    //         ObjectOutputStream oos = new ObjectOutputStream(baos);
    //         System.out.println("writeObject...");
    //         oos.writeObject(orderBook); // ERROR
    //         System.out.println("bytes");
    //         byte[] bytes = baos.toByteArray();

    //         oos.flush();
    //         oos.close();
            
    //         // Try to deserialize it back
    //         System.out.println("bais");
    //         ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
    //         System.out.println("ois");
    //         ObjectInputStream ois = new ObjectInputStream(bais);
    //         System.out.println("readObject...");
    //         OrderBook recovered = (OrderBook) ois.readObject();

    //         ois.close();
            
    //         // If we get here, serialization works!
    //         System.out.println("Serialization works! Symbol: " + recovered.symbol);
            
    //     } catch (Exception e) {
    //         System.out.println("Serialization failed: \n" + e.getMessage());
    //         e.printStackTrace();
    //     }
    // }
}