// package com.project.matchingengine.service;

// import com.project.matchingengine.models.order.Order;
// import com.project.matchingengine.models.order.OrderSide;
// import com.project.matchingengine.models.order.OrderStatus;
// import com.project.matchingengine.models.order.OrderType;
// import com.project.matchingengine.models.order.Trade;
// import com.project.matchingengine.service.OrderBook;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.springframework.boot.test.context.SpringBootTest;
// import static org.junit.jupiter.api.Assertions.*;

// import java.sql.Timestamp;
// import java.time.Instant;
// import java.util.ArrayList;
// import java.util.UUID;


// @SpringBootTest
// public class OrderBookTest {

//     private OrderBook orderBook;
//     private static final String SYMBOL = "AAPL";

//     @BeforeEach
//     void setUp() {
//         orderBook = new OrderBook(SYMBOL);
//     }

//     @Test
//     void testOrderBookInitialization() {
//         assertEquals(SYMBOL, orderBook.symbol);
//         assertTrue(orderBook.getTrades().isEmpty());
//     }

//     @Test
//     void testAddSingleBuyOrder() {
//         // Create a buy order
//         Order buyOrder = createOrder(OrderSide.BUY, 100.0, 10);
        
//         // Add to order book
//         orderBook.addOrder(buyOrder);
        
//         // Check the order status
//         assertEquals(OrderStatus.PENDING, buyOrder.status);
//         assertEquals(10, buyOrder.currentQuantity);
//         assertTrue(orderBook.getTrades().isEmpty());
//     }
    
//     @Test
//     void testAddSingleSellOrder() {
//         // Create a sell order
//         Order sellOrder = createOrder(OrderSide.SELL, 100.0, 10);
        
//         // Add to order book
//         orderBook.addOrder(sellOrder);
        
//         // Check the order status
//         assertEquals(OrderStatus.PENDING, sellOrder.status);
//         assertEquals(10, sellOrder.currentQuantity);
//         assertTrue(orderBook.getTrades().isEmpty());
//     }

//     @Test
//     void testMatchingBuyAndSellOrdersExactQuantity() {
//         // Create a buy order (higher price)
//         Order buyOrder = createOrder(OrderSide.BUY, 105.0, 10);
        
//         // Create a sell order (lower price)
//         Order sellOrder = createOrder(OrderSide.SELL, 100.0, 10);
        
//         // Add to order book
//         orderBook.addOrder(sellOrder);
//         orderBook.addOrder(buyOrder);
        
//         // Check matching result
//         assertEquals(OrderStatus.FILLED, buyOrder.status);
//         assertEquals(OrderStatus.FILLED, sellOrder.status);
//         assertEquals(0, buyOrder.currentQuantity);
//         assertEquals(0, sellOrder.currentQuantity);
        
//         // Check trades
//         ArrayList<Trade> trades = orderBook.getTrades();
//         assertEquals(1, trades.size());
//         Trade trade = trades.get(0);
//         assertEquals(SYMBOL, trade.symbol);
//         assertEquals(100.0, trade.price); // Should match at sell price
//         assertEquals(10, trade.quantity);
//         assertEquals(buyOrder.getOrderId(), trade.getBuyOrderId());
//         assertEquals(sellOrder.getOrderId(), trade.getSellOrderId());
//     }

//     @Test
//     void testPartialMatchBuyLargerThanSell() {
//         // Create a buy order with larger quantity
//         Order buyOrder = createOrder(OrderSide.BUY, 105.0, 15);
        
//         // Create a sell order with smaller quantity
//         Order sellOrder = createOrder(OrderSide.SELL, 100.0, 10);
        
//         // Add to order book
//         orderBook.addOrder(sellOrder);
//         orderBook.addOrder(buyOrder);
        
//         // Check matching result
//         assertEquals(OrderStatus.PARTIALLY_FILLED, buyOrder.status);
//         assertEquals(OrderStatus.FILLED, sellOrder.status);
//         assertEquals(5, buyOrder.currentQuantity);
//         assertEquals(0, sellOrder.currentQuantity);
        
//         // Check trades
//         ArrayList<Trade> trades = orderBook.getTrades();
//         assertEquals(1, trades.size());
//         assertEquals(10, trades.get(0).quantity);
//     }

//     @Test
//     void testPartialMatchSellLargerThanBuy() {
//         // Create a buy order with smaller quantity
//         Order buyOrder = createOrder(OrderSide.BUY, 105.0, 10);
        
//         // Create a sell order with larger quantity
//         Order sellOrder = createOrder(OrderSide.SELL, 100.0, 15);
        
//         // Add to order book
//         orderBook.addOrder(buyOrder);
//         orderBook.addOrder(sellOrder);
        
//         // Check matching result
//         assertEquals(OrderStatus.FILLED, buyOrder.status);
//         assertEquals(OrderStatus.PARTIALLY_FILLED, sellOrder.status);
//         assertEquals(0, buyOrder.currentQuantity);
//         assertEquals(5, sellOrder.currentQuantity);
        
//         // Check trades
//         ArrayList<Trade> trades = orderBook.getTrades();
//         assertEquals(1, trades.size());
//         assertEquals(10, trades.get(0).quantity);
//     }

//     @Test
//     void testNoMatchingDueToPrice() {
//         // Create a buy order with lower price
//         Order buyOrder = createOrder(OrderSide.BUY, 95.0, 10);
        
//         // Create a sell order with higher price
//         Order sellOrder = createOrder(OrderSide.SELL, 100.0, 10);
        
//         // Add to order book
//         orderBook.addOrder(buyOrder);
//         orderBook.addOrder(sellOrder);
        
//         // Check no matching occurred
//         assertEquals(OrderStatus.PENDING, buyOrder.status);
//         assertEquals(OrderStatus.PENDING, sellOrder.status);
//         assertEquals(10, buyOrder.currentQuantity);
//         assertEquals(10, sellOrder.currentQuantity);
//         assertTrue(orderBook.getTrades().isEmpty());
//     }

//     @Test
//     void testMultipleOrderMatching() {
//         // Create multiple buy orders
//         Order buy1 = createOrder(OrderSide.BUY, 105.0, 10);
//         Order buy2 = createOrder(OrderSide.BUY, 103.0, 5);
//         Order buy3 = createOrder(OrderSide.BUY, 101.0, 7);
        
//         // Create multiple sell orders
//         Order sell1 = createOrder(OrderSide.SELL, 100.0, 8);
//         Order sell2 = createOrder(OrderSide.SELL, 102.0, 12);
        
//         // Add to order book
//         orderBook.addOrder(buy1);
//         orderBook.addOrder(buy2);
//         orderBook.addOrder(buy3);
//         orderBook.addOrder(sell1);
//         orderBook.addOrder(sell2);
        
//         // Check matching results
//         assertEquals(OrderStatus.FILLED, buy1.status);
//         assertEquals(OrderStatus.FILLED, buy2.status);
//         assertEquals(OrderStatus.PARTIALLY_FILLED, buy3.status);
//         assertEquals(OrderStatus.FILLED, sell1.status);
//         assertEquals(OrderStatus.FILLED, sell2.status);
        
//         // Check remaining quantity for partially filled order
//         assertEquals(2, buy3.currentQuantity);
        
//         // Check trades
//         ArrayList<Trade> trades = orderBook.getTrades();
//         assertEquals(3, trades.size());
//         assertEquals(20, trades.stream().mapToInt(t -> t.quantity).sum());
//     }

//     @Test
//     void testMostRecent10Trades() {
//         // Generate 15 trades by creating matching orders
//         for (int i = 0; i < 15; i++) {
//             Order buy = createOrder(OrderSide.BUY, 110.0, 1);
//             Order sell = createOrder(OrderSide.SELL, 90.0, 1);
//             orderBook.addOrder(sell);
//             orderBook.addOrder(buy);
//         }
        
//         // Check all trades
//         assertEquals(15, orderBook.getTrades().size());
        
//         // Check recent 10 trades
//         ArrayList<Trade> recentTrades = orderBook.getMostRecent10Trades();
//         assertEquals(10, recentTrades.size());
        
//         // Make sure these are the most recent ones
//         for (int i = 0; i < 10; i++) {
//             assertEquals(orderBook.getTrades().get(i + 5), recentTrades.get(i));
//         }
//     }

//     // Helper method to create orders easily
//     private Order createOrder(OrderSide side, double price, int quantity) {
//         return new Order(
//             UUID.randomUUID(),
//             SYMBOL,
//             price,
//             quantity,
//             side,
//             OrderType.LIMIT,
//             price,
//             new Timestamp(Instant.now().toEpochMilli())
//         );
//     }
// }