// package com.project.matchingengine.tests;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.project.matchingengine.models.order.Order;
// import com.project.matchingengine.models.order.OrderSide;
// import com.project.matchingengine.models.order.OrderType;
// import com.project.matchingengine.models.order.OrderStatus;

// import io.lettuce.core.RedisClient;
// import io.lettuce.core.api.StatefulRedisConnection;
// import io.lettuce.core.api.sync.RedisCommands;

// import java.sql.Timestamp;
// import java.util.UUID;

// /**
//  * Test class for Redis connection and order storage using Lettuce
//  */
// public class RedisLettuceConnectTest {

//     private final RedisClient redisClient;
//     private final StatefulRedisConnection<String, String> connection;
//     private final RedisCommands<String, String> syncCommands;
//     private final ObjectMapper objectMapper;

//     public RedisLettuceConnectTest() {
//         System.out.println("Initializing Redis connection...");
//         this.redisClient = RedisClient.create("redis://localhost:6379");
//         this.connection = redisClient.connect();
//         this.syncCommands = connection.sync();
//         this.objectMapper = new ObjectMapper();
//         objectMapper.findAndRegisterModules();
//         System.out.println("Redis connection established successfully!");
//     }

//      /**
//      * Test basic Redis operations
//      */
//     public void runBasicTests() {
//         // Basic tests as before
//         System.out.println("\nTesting SET operation...");
//         String setResult = syncCommands.set("test-key", "Hello from Trading Engine!");
//         System.out.println("SET Result: " + setResult);

//         System.out.println("\nTesting GET operation...");
//         String getValue = syncCommands.get("test-key");
//         System.out.println("GET Result: " + getValue);

//         System.out.println("\nTesting EXISTS operation...");
//         Boolean existsResult = syncCommands.exists("test-key") > 0;
//         System.out.println("EXISTS Result: " + existsResult);

//         System.out.println("\nTesting DEL operation...");
//         Long delResult = syncCommands.del("test-key");
//         System.out.println("DEL Result: " + delResult);
//     }


//     /**
//      * Stores an Order object in Redis
//      * @param order The order to store
//      * @return true if successful, false otherwise
//      */
//     public boolean storeOrder(Order order) {
//         try {
//             UUID orderId = order.getOrderId();
            
//             String orderKey = "order:" + orderId.toString();
//             System.out.println("Storing order with ID: " + orderId.toString());
            
//             // Convert Order to JSON string
//             String orderJson = objectMapper.writeValueAsString(order);
            
//             // Store in Redis
//             String result = syncCommands.set(orderKey, orderJson);
            
//             return "OK".equals(result);
//         } catch (Exception e) {
//             System.err.println("Error storing order: " + e.getMessage());
//             e.printStackTrace();
//             return false;
//         }
//     }

//     /**
//      * Retrieves an Order from Redis by ID
//      * @param orderId The order ID
//      * @return The Order object or null if not found
//      */
//     public Order getOrder(UUID orderId) {
//         try {
//             String orderKey = "order:" + orderId.toString();

//             System.out.println("Retrieving order with ID: " + orderId.toString());
            
//             // Get JSON from Redis
//             String orderJson = syncCommands.get(orderKey);
            
//             if (orderJson == null) {
//                 System.out.println("Order not found with ID: " + orderId.toString());
//                 return null;
//             } else {
//                 System.out.println("Retrieved order with ID: " + orderId.toString());
//             }
            
//             // Convert JSON to Order object
//             System.out.println("Order JSON: " + orderJson);
//             return objectMapper.readValue(orderJson, Order.class);
//         } catch (Exception e) {
//             System.err.println("Error retrieving order: " + e.getMessage());
//             e.printStackTrace();
//             return null;
//         }
//     }

//     /**
//      * Deletes an order from Redis
//      * @param orderId The order ID to delete
//      * @return true if deleted, false otherwise
//      */
//     public boolean deleteOrder(UUID orderId) {
//         String orderKey = "order:" + orderId.toString();
//         Long result = syncCommands.del(orderKey);
//         return result > 0;
//     }

//     /**
//      * Test order storage and retrieval
//      */
//     public void testOrderOperations() {
//         try {
//             System.out.println("\n--- Testing Order Operations ---");
            
//             // Create a test order
//             Order testOrder = new Order(UUID.randomUUID(),
//                                         "AAPL",
//                                         100.0,
//                                         100,
//                                         OrderSide.BUY,
//                                         OrderType.LIMIT,
//                                         0.0,
//                                         new Timestamp(System.currentTimeMillis()));     

//             // Store order
//             System.out.println("Storing test order...");
//             boolean storedSuccessfully = storeOrder(testOrder);
//             System.out.println("Order stored successfully: " + storedSuccessfully);
            
//             // Retrieve order
//             System.out.println("\nRetrieving test order...");
//             Order retrievedOrder = getOrder(testOrder.getOrderId());
//             if (retrievedOrder != null) {
//                 // System.out.println("Retrieved order: " + retrievedOrder);
//                 System.out.println("Order ID: " + retrievedOrder.getOrderId());
//                 System.out.println("Order Price: " + retrievedOrder.getPrice());
//                 System.out.println("Order Side: " + retrievedOrder.getSide());
//             }
            
//             // Delete order
//             System.out.println("\nDeleting test order...");
//             boolean deletedSuccessfully = deleteOrder(testOrder.getOrderId());
//             System.out.println("Order deleted successfully: " + deletedSuccessfully);
            
//             // Verify deletion
//             Order afterDeletion = getOrder(testOrder.getOrderId());
//             System.out.println("\nOrder after deletion: " + afterDeletion + " (should be null)");
            
//         } catch (Exception e) {
//             System.err.println("Error in order operations test: " + e.getMessage());
//             e.printStackTrace();
//         }
//     }

//     /**
//      * Clean up resources
//      */
//     public void shutdown() {
//         System.out.println("\nClosing Redis connection...");
//         if (connection != null) {
//             connection.close();
//         }
        
//         System.out.println("Shutting down Redis client...");
//         if (redisClient != null) {
//             redisClient.shutdown();
//         }
        
//         System.out.println("Redis cleanup complete!");
//     }

//     /**
//      * Main method to run the Redis connection test
//      */
//     public static void main(String[] args) {
//         RedisLettuceConnectTest test = null;
        
//         try {
//             // Create test instance
//             test = new RedisLettuceConnectTest();
            
//             // Run basic Redis operations test
//             test.runBasicTests();
            
//             // Run order operations test
//             test.testOrderOperations();
            
//         } catch (Exception e) {
//             System.err.println("Error in Redis test: " + e.getMessage());
//             e.printStackTrace();
//         } finally {
//             // Always clean up resources
//             if (test != null) {
//                 test.shutdown();
//             }
//         }
//     }
// }