// package com.project.matchingengine.models;

// import com.project.matchingengine.models.order.Order;
// import com.project.matchingengine.models.order.OrderSide;
// import com.project.matchingengine.models.order.OrderType;
// import com.project.matchingengine.models.order.OrderStatus;

// import org.junit.jupiter.api.Test;
// import org.springframework.boot.test.context.SpringBootTest;
// import static org.junit.jupiter.api.Assertions.*;

// import java.time.Instant;
// import java.util.UUID;
// import java.sql.Timestamp;


// @SpringBootTest
// public class TestOrderCreation {
//     public static void main(String[] args) {
//         // Create a new order
//         Order order = new Order(
//             UUID.randomUUID(),
//             "AAPL",
//             150.0,
//             100,
//             OrderSide.BUY,
//             OrderType.MARKET,
//             0.0,
//             new Timestamp(System.currentTimeMillis())
//         );

//         // Print order details
//         System.out.println("Order ID: " + order.getOrderId());
//         System.out.println("Symbol: " + order.getSymbol());
//         System.out.println("Price: " + order.getPrice());
//         System.out.println("Original Quantity: " + order.getOriginalQuantity());
//         System.out.println("Current Quantity: " + order.currentQuantity);
//         System.out.println("Side: " + order.getSide());
//         System.out.println("Type: " + order.getType());
//         System.out.println("Limit Price: " + order.getLimitPrice());
//     }

//     @Test
//     public void testOrderCreation() {
//         // Create a new buy order
//         UUID orderId = UUID.randomUUID();
//         String symbol = "AAPL";
//         double price = 150.0;
//         int quantity = 100;
//         OrderSide side = OrderSide.BUY;
//         OrderType type = OrderType.MARKET;
//         double limitPrice = 0.0; // For market orders, limit price is not applicable
//         Timestamp orderTimestamp = new Timestamp(System.currentTimeMillis());
//         Order order = new Order(orderId, symbol, price, quantity, side, type, limitPrice, orderTimestamp);

        
//         // Assert all fields are set correctly
//         assertEquals(orderId, order.getOrderId());
//         assertEquals(symbol, order.getSymbol());
//         assertEquals(price, order.getPrice());
//         assertEquals(quantity, order.getOriginalQuantity());
//         assertNotNull(order.getOrderTimestamp(), "Timestamp should be automatically set");
//     }
    
//     // @Test
//     // public void testOrderEquality() {
//     //     Order order1 = new Order(123e4567-e89b-42d3-a456-556642440000, "AAPL", 150.0, 100, OrderSide.BUY, OrderType.MARKET, 0.0, new Timestamp(System.currentTimeMillis()));
//     //     Order order2 = new Order("123e4567-e89b-42d3-a456-556642440000", "AAPL", 150.0, 100, OrderSide.BUY, OrderType.MARKET, 0.0, new Timestamp(System.currentTimeMillis()));
//     //     Order order3 = new Order("123e4567-e89b-42d3-a234-556642441111", "AAPL", 150.0, 100, OrderSide.BUY, OrderType.MARKET, 0.0, new Timestamp(System.currentTimeMillis()));
        
//     //     // Orders with same ID should be equal
//     //     assertEquals(order1, order2);
//     //     assertNotEquals(order1, order3);
//     // }
    
//     // @Test
//     // public void testOrderValidation() {
//     //     // Test with invalid parameters
//     //     assertThrows(IllegalArgumentException.class, () -> 
//     //         new Order("", "AAPL", 150.0, 100, true), 
//     //         "Empty order ID should throw exception");
            
//     //     assertThrows(IllegalArgumentException.class, () -> 
//     //         new Order("ORD123", "", 150.0, 100, true), 
//     //         "Empty symbol should throw exception");
            
//     //     assertThrows(IllegalArgumentException.class, () -> 
//     //         new Order("ORD123", "AAPL", -10.0, 100, true), 
//     //         "Negative price should throw exception");
            
//     //     assertThrows(IllegalArgumentException.class, () -> 
//     //         new Order("ORD123", "AAPL", 150.0, 0, true), 
//     //         "Zero quantity should throw exception");
//     // }

// }