// package com.project.matchingengine.tests;

// import java.sql.Timestamp;
// import java.util.UUID;

// import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
// import org.springframework.data.redis.core.RedisTemplate;
// import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
// import org.springframework.data.redis.serializer.StringRedisSerializer;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.project.matchingengine.models.order.Order;
// import com.project.matchingengine.models.order.OrderSide;
// import com.project.matchingengine.models.order.OrderType;
// import com.project.matchingengine.service.OrderBookCacheService;

// /**
//  * Test class for OrderBookService Redis caching operations
//  */
// public class OrderBookServiceTest {

//     private final LettuceConnectionFactory connectionFactory;
//     private final RedisTemplate<String, Object> redisTemplate;
//     private final OrderBookCacheService orderBookService;
//     private final ObjectMapper objectMapper;

//     public OrderBookServiceTest() {
//         System.out.println("Initializing Redis connection and OrderBookService...");
        
//         // Initialize connection factory
//         this.connectionFactory = new LettuceConnectionFactory("localhost", 6379);
//         this.connectionFactory.afterPropertiesSet();
        
//         // Configure Redis template with appropriate serializers
//         this.redisTemplate = new RedisTemplate<>();
//         this.redisTemplate.setConnectionFactory(connectionFactory);
//         this.redisTemplate.setKeySerializer(new StringRedisSerializer());
//         this.redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
//         this.redisTemplate.afterPropertiesSet();
        
//         // Create the service to test
//         this.orderBookService = new OrderBookCacheService(redisTemplate);
        
//         // Initialize object mapper for tests
//         this.objectMapper = new ObjectMapper();
//         objectMapper.findAndRegisterModules();
        
//         System.out.println("OrderBookService test setup completed successfully!");
//     }

//     /**
//      * Creates a sample OrderBook for testing
//      * @param symbol The trading symbol for the order book
//      * @return A populated OrderBook
//      */

//     public Order createOrder(String symbol, double price, int quantity, OrderSide side) {
//         return new Order(
//             UUID.randomUUID(),
//             symbol,
//             price,
//             quantity,
//             side,
//             OrderType.MARKET,
//             0.0,
//             new Timestamp(System.currentTimeMillis())
//         );
//     }
    
//     /**
//      * Test caching an OrderBook
//      */
//     public void testCacheOrder() {
//         String symbol = "AAPL";
//         System.out.println("\n--- Testing OrderBook Caching ---");
        
//         // Create a sample order book
//         Order order1 = createOrder(symbol, 99.5, 10, OrderSide.BUY);
//         Order order2 = createOrder(symbol, 100.5, 8, OrderSide.SELL);
//         Order order3 = createOrder(symbol, 99.0, 15, OrderSide.BUY);
//         Order order4 = createOrder(symbol, 101.0, 12, OrderSide.SELL);

        
//         try {
//             // Cache the order book
//             // System.out.println("Caching order book...");
//             System.out.println("Caching sample order book for " + symbol + " ...");
//             orderBookService.processOrder(order1);
//             System.out.println("Order 1 cached successfully\n");
//             orderBookService.processOrder(order2);
//             System.out.println("Order 2 cached successfully\n");
//             orderBookService.processOrder(order3);
//             System.out.println("Order 3 cached successfully\n");
//             orderBookService.processOrder(order4);
//             System.out.println("Order 4 cached successfully\n");
            
//             // Verify exists using RedisTemplate directly
//             boolean exists = redisTemplate.hasKey("order_book:" + symbol);
//             System.out.println("Order book " + symbol + " exists in Redis: " + exists);
            
//         } catch (Exception e) {
//             System.err.println("Error caching order: " + e.getMessage());
//             e.printStackTrace();
//         }
//     }

//     /**
//      * Test retrieving an OrderBook
//      */
//     // public void testGetOrderBook() {
//     //     String symbol = "AAPL";
//     //     System.out.println("\n--- Testing OrderBook Retrieval ---");
        
//     //     try {
//     //         // Retrieve the order book
//     //         System.out.println("Retrieving order book for " + symbol + "...");
//     //         Optional<OrderBook> retrievedOrderBookOpt = orderBookService.getOrderBook(symbol);
            
//     //         if (retrievedOrderBookOpt.isPresent()) {
//     //             OrderBook retrievedOrderBook = retrievedOrderBookOpt.get();
//     //             System.out.println("Order book retrieved successfully");
//     //             System.out.println("Symbol: " + retrievedOrderBook.symbol);
//     //             System.out.println("Buy orders: " + retrievedOrderBook.getBuyOrdersList().size());
//     //             System.out.println("Sell orders: " + retrievedOrderBook.getSellOrdersList().size());
                
//     //             // // Print book depth
//     //             // System.out.println("\nBuy side order book depth:");
//     //             // retrievedOrderBook.getBuyOrdersList().forEach((price, orders) -> 
//     //             //     System.out.println("Price: " + price + ", Volume: " + orders.stream().mapToInt(Order::getQuantity).sum()));
                
//     //             // System.out.println("\nSell side order book depth:");
//     //             // retrievedOrderBook.getSellOrders().forEach((price, orders) -> 
//     //             //     System.out.println("Price: " + price + ", Volume: " + orders.stream().mapToInt(Order::getQuantity).sum()));
//     //         } else {
//     //             System.out.println("Order book not found for symbol: " + symbol);
//     //         }
            
//     //     } catch (Exception e) {
//     //         System.err.println("Error retrieving order book: " + e.getMessage());
//     //         e.printStackTrace();
//     //     }
//     // }

//     // /**
//     //  * Test evicting an OrderBook
//     //  */
//     // public void testEvictOrderBook() {
//     //     String symbol = "AAPL";
//     //     System.out.println("\n--- Testing OrderBook Eviction ---");
        
//     //     try {
//     //         // Evict the order book
//     //         System.out.println("Evicting order book for " + symbol + "...");
//     //         orderBookService.evictOrderBook(symbol);
//     //         System.out.println("Order book evicted");
            
//     //         // Verify it's gone
//     //         Optional<OrderBook> retrievedOrderBookOpt = orderBookService.getOrderBook(symbol);
//     //         System.out.println("Order book exists after eviction: " + retrievedOrderBookOpt.isPresent() + " (should be false)");
            
//     //         // Double-check with Redis directly
//     //         boolean exists = redisTemplate.hasKey("order_book:" + symbol);
//     //         System.out.println("Order book exists in Redis after eviction: " + exists + " (should be false)");
            
//     //     } catch (Exception e) {
//     //         System.err.println("Error evicting order book: " + e.getMessage());
//     //         e.printStackTrace();
//     //     }
//     // }

//     // /**
//     //  * Test the full lifecycle of an OrderBook in cache
//     //  */
//     // public void testOrderBookCacheLifecycle() {
//     //     String symbol = "MSFT";
//     //     System.out.println("\n--- Testing OrderBook Cache Lifecycle ---");
        
//     //     try {
//     //         // First ensure the order book doesn't exist
//     //         orderBookService.evictOrderBook(symbol);
//     //         System.out.println("Ensured order book is not in cache");
            
//     //         // Verify it doesn't exist
//     //         Optional<OrderBook> initialCheck = orderBookService.getOrderBook(symbol);
//     //         System.out.println("Initial check - Order book exists: " + initialCheck.isPresent() + " (should be false)");
            
//     //         // Create and cache an order book
//     //         OrderBook orderBook = createSampleOrderBook(symbol);
//     //         System.out.println("Created sample order book with " + 
//     //             orderBook.getBuyOrdersList().size() + " buy price levels and " +
//     //             orderBook.getSellOrdersList().size() + " sell price levels");
            
//     //         // Cache it
//     //         orderBookService.cacheOrderBook(symbol, orderBook);
//     //         System.out.println("Cached the order book");
            
//     //         // Retrieve it
//     //         Optional<OrderBook> retrievedOpt = orderBookService.getOrderBook(symbol);
//     //         if (retrievedOpt.isPresent()) {
//     //             OrderBook retrieved = retrievedOpt.get();
//     //             System.out.println("Retrieved order book successfully");
//     //             System.out.println("Retrieved buy orders size: " + retrieved.getBuyOrdersList().size());
//     //             System.out.println("Retrieved sell orders size: " + retrieved.getSellOrdersList().size());
                
//     //             // Validate data integrity
//     //             boolean dataMatchesBuy = retrieved.getBuyOrdersList().size() == orderBook.getBuyOrdersList().size();
//     //             boolean dataMatchesSell = retrieved.getSellOrdersList().size() == orderBook.getSellOrdersList().size();
//     //             System.out.println("Data integrity check: " + (dataMatchesBuy && dataMatchesSell ? "PASSED" : "FAILED"));
//     //         } else {
//     //             System.out.println("FAILED: Could not retrieve the cached order book");
//     //         }
            
//     //         // Finally, clean up
//     //         orderBookService.evictOrderBook(symbol);
//     //         System.out.println("Cleaned up by evicting order book");
            
//     //         // Verify cleanup
//     //         Optional<OrderBook> finalCheck = orderBookService.getOrderBook(symbol);
//     //         System.out.println("Final check - Order book exists: " + finalCheck.isPresent() + " (should be false)");
            
//     //     } catch (Exception e) {
//     //         System.err.println("Error in order book lifecycle test: " + e.getMessage());
//     //         e.printStackTrace();
//     //     }
//     // }

//     /**
//      * Clean up resources
//      */
//     public void shutdown() {
//         System.out.println("\nShutting down Redis connection...");
//         if (connectionFactory != null) {
//             connectionFactory.destroy();
//         }
//         System.out.println("Redis cleanup complete!");
//     }

//     /**
//      * Main method to run the OrderBookService tests
//      */
//     public static void main(String[] args) {
//         OrderBookServiceTest test = null;
        
//         try {
//             // Create test instance
//             test = new OrderBookServiceTest();
            
//             // Run the cache operation tests
//             test.testCacheOrder();
//             // test.testGetOrderBook();
//             // test.testEvictOrderBook();
            
//             // // Run the full lifecycle test
//             // test.testOrderBookCacheLifecycle();
            
//         } catch (Exception e) {
//             System.err.println("Error in OrderBookService test: " + e.getMessage());
//             e.printStackTrace();
//         } finally {
//             // Always clean up resources
//             if (test != null) {
//                 test.shutdown();
//             }
//         }
//     }
// }