// package com.project.matchingengine.service;

// import java.time.Duration;
// import java.util.UUID;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.data.redis.core.RedisOperations;
// import org.springframework.data.redis.core.RedisTemplate;
// import org.springframework.data.redis.core.SessionCallback;
// import org.springframework.stereotype.Service;

// import com.project.matchingengine.models.order.Order;


// /* 
//     Service class to handle order book caching and transactions in Redis
//     This provides atomic operations for order processing and order book management

//     Process a new order with Redis transaction to ensure atomicity
    
//     Steps:
//         1. Check if order already exists (duplicate check)
//         2. Get the order book for the symbol
//         3. Add the order to the order book (which may create trades)
//         4. Store any resulting trades
//         5. Update the order book in Redis
//         6. Store the order for future duplicate checking
    
//     @param order The order to process
//     @return true if order processed successfully, false if it was a duplicate
// */
// @Service
// public class OrderBookCacheService {
    
//     private static final Logger logger = LoggerFactory.getLogger(OrderBookCacheService.class);

//     private static final String ORDER_BOOK_PREFIX = "order_book:";
//     private static final String ORDER_PREFIX = "order:";
//     private static final Duration ORDER_BOOK_EXPIRATION = Duration.ofHours(24); // Orders expire after 1 day
//     private static final Duration ORDER_EXPIRATION = Duration.ofHours(24);

//     private final RedisTemplate<String, Object> redisTemplate;

//     public OrderBookCacheService(RedisTemplate<String, Object> redisTemplate) {
//         this.redisTemplate = redisTemplate;
//     }

//     public void processOrder(Order order) {
//         String orderId = order.getOrderId().toString();
//         String orderBookKey = ORDER_BOOK_PREFIX + order.getSymbol();
//         String orderKey = ORDER_PREFIX + orderId;

//         redisTemplate.execute(new SessionCallback<Void>() { //TODO: ERROR HANDLING
//             @Override
//             public Void execute(RedisOperations operations) {

//                 operations.watch(orderKey); 
//                 operations.watch(orderBookKey);

//                 operations.multi(); // Start transaction

//                 try {
//                     // Check for duplicate orders
//                     System.out.println("Detecting duplicated order");


//                     if (Boolean.TRUE.equals(operations.hasKey(orderKey))) {
//                         logger.info("Duplicate order detected: {}", orderId);
//                         operations.unwatch(); // Discard transaction
//                         return null; // Exit early
//                     }
//                     System.out.println("No duplicated order");


//                     // Get the current order book
//                     OrderBook orderBook = (OrderBook) operations.opsForValue().get(orderBookKey);

//                     if (orderBook == null) {
//                         System.out.println("Creating new order book for symbol: " + order.getSymbol());
//                         orderBook = new OrderBook(order.getSymbol());
//                     }

//                     // Add the new order to the order book
//                     orderBook.addOrder(order);

//                     // Store the updated order book back in Redis
//                     System.out.println("Storing order book for symbol: " + order.getSymbol());
//                     operations.opsForValue().set(orderBookKey, orderBook, ORDER_BOOK_EXPIRATION);
                    

//                     // Store only the new orderId for future duplicate checking
//                     System.out.println("Storing symbol");
//                     operations.opsForHash().put(orderKey, "symbol", order.getSymbol());
//                     System.out.println("Storing price");
//                     operations.opsForHash().put(orderKey, "price", order.getPrice());
//                     System.out.println("Storing qunatity");
//                     operations.opsForHash().put(orderKey, "quantity", order.getOriginalQuantity());
//                     System.out.println("Storing side");
//                     operations.opsForHash().put(orderKey, "side", order.getSide().toString());
//                     System.out.println("Storing type");
//                     operations.opsForHash().put(orderKey, "type", order.getType().toString());
//                     System.out.println("Storing limit price");
//                     operations.opsForHash().put(orderKey, "limit price", order.getLimitPrice());
//                     System.out.println("Storing timestamp");
//                     operations.opsForHash().put(orderKey, "timestamp", order.getOrderTimestamp().toString());
//                     System.out.println("Adding expiration");
//                     operations.expire(orderKey, ORDER_EXPIRATION); 
//                     System.out.println("DONE storing order");
                    
//                     System.out.println("Executing transaction for order: " + orderId);
//                     operations.exec(); // Execute transaction
//                     System.out.println("Done executing order: " + orderId);
//                     return null;
                
//                 } catch (Exception e) {
//                     logger.error("Error processing order {}: {}", orderId, e.getMessage());
//                     operations.unwatch(); 
//                     operations.discard(); // Discard transaction on error
//                     return null; // Exit early
//                 }
//             }
//         });
//     }

//     public OrderBook getOrderBook(String symbol) {
//         if (symbol == null || symbol.isEmpty()) {
//             logger.warn("Symbol is null or empty");
//             return null;
//         }
//         String orderBookKey = ORDER_BOOK_PREFIX + symbol;
//         return (OrderBook) redisTemplate.opsForValue().get(orderBookKey);
//     }


//     // public Order getOrder(UUID orderId) {
//     //     String key = ORDER_PREFIX + orderId.toString();
//     //     if (!redisTemplate.opsForHash().hasKey(key, "symbol")) {
//     //         logger.info("Order with ID {} not found in cache", orderId);
//     //         return null;
//     //     }
//     //     String symbol = (String) redisTemplate.opsForHash().get(key, "symbol");
//     //     Double price = (Double) redisTemplate.opsForHash().get(key, "price");
//     //     int quantity = (Integer) redisTemplate.opsForHash().get(key, "quantity");
//     //     OrderSide side = OrderSide.fromString((String) redisTemplate.opsForHash().get(key, "side"));
//     //     OrderType type = OrderType.fromString((String) redisTemplate.opsForHash().get(key, "type"));
//     //     return new Order(
//     //         orderId,
//     //         symbol,
//     //         Double.parseDouble(price),
//     //         Integer.parseInt(quantity),
//     //         null, // Assuming OrderSide is not needed here
//     //         null, // Assuming OrderType is not needed here
//     //         0.0, // Assuming limit price is not needed here
//     //         null // Assuming order timestamp is not needed here
//     //     );
//     //     // return Optional.ofNullable(order);
//     // }


//     public void evictOrderBook(String symbol) {
//         String key = ORDER_BOOK_PREFIX + symbol;
//         redisTemplate.delete(key);
//     }

//     public void evictOrder(UUID orderId) {
//         String key = ORDER_PREFIX + orderId.toString();
//         redisTemplate.delete(key);
//     }




    
// }

