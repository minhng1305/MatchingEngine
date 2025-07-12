package com.project.matchingengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    
    @Bean
    public ObjectMapper objectMapper() {
        // return new ObjectMapper();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}

// import java.sql.Timestamp;
// import java.util.UUID;
// import java.util.concurrent.CountDownLatch;
// import java.util.concurrent.TimeUnit;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.boot.CommandLineRunner;
// import org.springframework.boot.SpringApplication;
// import org.springframework.boot.autoconfigure.SpringBootApplication;
// import org.springframework.context.ConfigurableApplicationContext;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.ComponentScan;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.project.matchingengine.models.order.Order;
// import com.project.matchingengine.models.order.OrderSide;
// import com.project.matchingengine.models.order.OrderType;
// import com.project.matchingengine.service.kafka.KafkaConsumer;
// import com.project.matchingengine.service.kafka.KafkaProducer;

// @SpringBootApplication
// @ComponentScan(basePackages = {
//     "com.project.matchingengine.config",
//     "com.project.matchingengine.service.kafka", 
//     "com.project.matchingengine"
// })
// public class Application {
    
//     private static final Logger logger = LoggerFactory.getLogger(Application.class);
    
//     public static void main(String[] args) {
//         ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
        
//         // Add a shutdown hook to close the context when the app is terminated
//         Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//             logger.info("Shutting down the application...");
//             context.close();
//         }));
//     }
    
//     @Bean
//     public ObjectMapper objectMapper() {
//         return new ObjectMapper();
//     }
    
//     // @Bean
//     // public CommandLineRunner demoKafka(KafkaProducer kafkaProducer, KafkaConsumer orderConsumer) {
//     //     System.out.println("Starting Kafka Producer Demo...");
//     //     logger.info("Starting Kafka Producer Demo...");
//     //     return args -> {
//     //         logger.info("====== STARTING KAFKA CONSUMER DEMO ======");
//     //         logger.info("This demo will produce two orders and consume them from Kafka");
            
//     //         // Set up a latch to wait for messages to be consumed
//     //         CountDownLatch latch = new CountDownLatch(2);
//     //         orderConsumer.setLatch(latch);
            
//     //         // Create and send sample order
//     //         Order buyOrder1 = createOrder("AAPL", 150.0, 100, OrderSide.BUY);
//     //         Order sellOrder1 = createOrder("AAPL", 148.0, 50, OrderSide.SELL);
//     //         Order buyOrder2 = createOrder("AAPL", 149.5, 200, OrderSide.BUY);
//     //         Order sellOrder2 = createOrder("AAPL", 149.0, 150, OrderSide.SELL);
            
//     //         logger.info("Sending BUY order: {}", buyOrder1.getOrderId());
//     //         kafkaProducer.sendOrder(buyOrder1);
            
//     //         logger.info("Sending SELL order: {}", sellOrder1.getOrderId());
//     //         kafkaProducer.sendOrder(sellOrder1);

//     //         logger.info("Sending BUY order: {}", buyOrder2.getOrderId());
//     //         kafkaProducer.sendOrder(buyOrder2);
            
//     //         logger.info("Sending SELL order: {}", sellOrder2.getOrderId());
//     //         kafkaProducer.sendOrder(sellOrder2);
            
//     //         kafkaProducer.flushProducer();
//     //         logger.info("Orders sent to Kafka. Waiting for consumer to process them...");
            
//     //         // Wait for the consumer to receive both messages
//     //         boolean receivedAllMessages = latch.await(15, TimeUnit.SECONDS);
            
//     //         if (receivedAllMessages) {
//     //             logger.info("====== DEMO COMPLETED SUCCESSFULLY ======");
//     //             logger.info("Successfully produced and consumed 4 orders through Kafka!");
//     //         } else {
//     //             logger.error("====== DEMO TIMED OUT ======");
//     //             logger.error("Didn't receive all expected messages within the timeout period!");
//     //             logger.error("Received {} out of 2 expected messages", 2 - latch.getCount());
//     //         }
            
//     //         // Wait a moment before shutting down to make sure logs are flushed
//     //         Thread.sleep(2000);
            
//     //         // Exit the application
//     //         System.exit(0);
//     //     };
//     // }

//     @Bean
//     public CommandLineRunner demoKafka(KafkaProducer kafkaProducer, KafkaConsumer orderConsumer) {
//         return args -> {
//             // =================================================================
//             // 1. SETUP FOR BENCHMARKING
//             // =================================================================
//             final int numberOfOrders = 1000; // Define the number of orders to send
//             logger.info("====== STARTING KAFKA BENCHMARK ======");
//             logger.info("This demo will produce and consume {} orders to measure performance.", numberOfOrders);
            
//             // Set up a latch to wait for all messages to be consumed by Kafka
//             // This is crucial for knowing when the process is complete.
//             CountDownLatch latch = new CountDownLatch(numberOfOrders);
//             orderConsumer.setLatch(latch);
            
//             // =================================================================
//             // 2. START TIMER and SUBMIT ORDERS
//             // =================================================================
//             logger.info("Starting to submit {} orders...", numberOfOrders);
            
//             // Record the start time to measure total processing duration
//             long startTime = System.nanoTime();
            
//             // Loop to create and send 1000 orders
//             for (int i = 0; i < numberOfOrders; i++) {
//                 // Alternate between BUY and SELL orders for variety
//                 OrderSide side = (i % 2 == 0) ? OrderSide.BUY : OrderSide.SELL;
//                 double price = 150.0 + (i * 0.01); // Vary the price slightly
                
//                 Order order = createOrder("AAPL", price, 100, side);
                
//                 // Send the order to the Kafka topic
//                 kafkaProducer.sendOrder(order);
//             }
            
//             // Ensure all buffered records are sent to Kafka before proceeding
//             kafkaProducer.flushProducer();
//             logger.info("All {} orders have been sent to Kafka. Waiting for consumer to process them...", numberOfOrders);
            
//             // =================================================================
//             // 3. AWAIT COMPLETION AND CALCULATE RESULTS
//             // =================================================================
            
//             // Wait for the consumer to receive all messages, with a timeout
//             // The timeout is increased to allow sufficient time for 1000 orders.
//             boolean allMessagesReceived = latch.await(60, TimeUnit.SECONDS);
            
//             // Record the end time after the latch is released (or times out)
//             long endTime = System.nanoTime();
            
//             // Calculate the total duration in milliseconds
//             long durationMillis = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
            
//             if (allMessagesReceived) {
//                 // Calculate throughput (orders per second)
//                 double throughput = (double) numberOfOrders / (durationMillis / 1000.0);
                
//                 logger.info("====== BENCHMARK COMPLETED SUCCESSFULLY ======");
//                 logger.info("Successfully produced and consumed {} orders.", numberOfOrders);
//                 logger.info("Total time taken: {} ms", durationMillis);
//                 logger.info("Throughput: {} orders/second", String.format("%.2f", throughput));
//             } else {
//                 long receivedCount = numberOfOrders - latch.getCount();
//                 logger.error("====== BENCHMARK TIMED OUT ======");
//                 logger.error("Did not receive all messages within the 60-second timeout period.");
//                 logger.error("Received {} out of {} expected messages.", receivedCount, numberOfOrders);
//                 logger.error("Total time elapsed before timeout: {} ms", durationMillis);
//             }
            
//             // Wait a moment before shutting down to ensure logs are flushed
//             Thread.sleep(2000);
            
//             // Exit the application
//             System.exit(0);
//         };
//     }
    
//     private static Order createOrder(String symbol, double price, int quantity, OrderSide side) {
//         return new Order(
//             UUID.randomUUID(),
//             symbol,
//             price,
//             quantity,
//             side,
//             OrderType.LIMIT,
//             price,
//             new Timestamp(System.currentTimeMillis())
//         );
//     }
    
// }
