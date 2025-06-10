package com.project.matchingengine.tests;

import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.models.order.OrderSide;
import com.project.matchingengine.models.order.OrderType;
import com.project.matchingengine.service.kafka.KafkaProducer;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.project.matchingengine.service.kafka", 
    "com.project.matchingengine.tests"
})
public class KafkaProducerDemo {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerDemo.class);
    
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(KafkaProducerDemo.class, args);
        
        // Add a shutdown hook to close the context when the app is terminated
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down the application...");
            context.close();
        }));
    }
    
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
    
    @Bean
    public CommandLineRunner demoKafka(KafkaProducer kafkaProducer, OrderConsumerDemo orderConsumer) {
        System.out.println("Starting Kafka Producer Demo...");
        logger.info("Starting Kafka Producer Demo...");
        return args -> {
            logger.info("====== STARTING KAFKA CONSUMER DEMO ======");
            logger.info("This demo will produce two orders and consume them from Kafka");
            
            // Set up a latch to wait for messages to be consumed
            CountDownLatch latch = new CountDownLatch(2);
            orderConsumer.setLatch(latch);
            
            // Create and send two sample orders
            Order buyOrder = createSampleOrder(OrderSide.BUY);
            Order sellOrder = createSampleOrder(OrderSide.SELL);
            
            logger.info("Sending BUY order: {}", buyOrder.getOrderId());
            kafkaProducer.sendOrder(buyOrder);
            
            logger.info("Sending SELL order: {}", sellOrder.getOrderId());
            kafkaProducer.sendOrder(sellOrder);
            
            kafkaProducer.flushProducer();
            logger.info("Orders sent to Kafka. Waiting for consumer to process them...");
            
            // Wait for the consumer to receive both messages
            boolean receivedAllMessages = latch.await(30, TimeUnit.SECONDS);
            
            if (receivedAllMessages) {
                logger.info("====== DEMO COMPLETED SUCCESSFULLY ======");
                logger.info("Successfully produced and consumed 2 orders through Kafka!");
            } else {
                logger.error("====== DEMO TIMED OUT ======");
                logger.error("Didn't receive all expected messages within the timeout period!");
                logger.error("Received {} out of 2 expected messages", 2 - latch.getCount());
            }
            
            // Wait a moment before shutting down to make sure logs are flushed
            Thread.sleep(2000);
            
            // Exit the application
            System.exit(0);
        };
    }
    
    private Order createSampleOrder(OrderSide side) {
        UUID orderId = UUID.randomUUID();
        String symbol = "AAPL";
        double price = side == OrderSide.BUY ? 150.0 : 151.0;
        int quantity = 100;
        OrderType type = OrderType.LIMIT;
        double limitPrice = price;
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        
        return new Order(orderId, symbol, price, quantity, side, type, limitPrice, timestamp);
    }
    
    @Component
    public static class OrderConsumerDemo {
        private final Logger logger = LoggerFactory.getLogger(OrderConsumerDemo.class);
        private final ObjectMapper objectMapper;
        private CountDownLatch latch;
        
        @Autowired
        public OrderConsumerDemo(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }
        
        public void setLatch(CountDownLatch latch) {
            this.latch = latch;
        }
        
        @KafkaListener(topics = "${app.kafka.topics.order-submission}", groupId = "${spring.kafka.consumer.group-id}")
        public void consumeOrder(String orderJson) {
            try {
                Order order = objectMapper.readValue(orderJson, Order.class);
                logger.info("==> CONSUMER: Received order: {}, side: {}, symbol: {}, price: {}, quantity: {}",
                        order.getOrderId(),
                        order.getSide(),
                        order.getSymbol(),
                        order.getPrice(),
                        order.getOriginalQuantity());
                
                // Count down the latch to signal message received
                if (latch != null) {
                    latch.countDown();
                    logger.info("   Remaining orders to receive: {}", latch.getCount());
                }
            } catch (Exception e) {
                logger.error("Failed to process order: {}", e.getMessage(), e);
            }
        }
    }
}

    // public static void main(String[] args) {
    //     ConfigurableApplicationContext context = SpringApplication.run(KafkaProducerDemo.class, args);
        
    //     try {
    //         // Get the KafkaProducer bean
    //         KafkaProducer kafkaProducer = context.getBean(KafkaProducer.class);

    //         // Test 1: Send a simple message
    //         System.out.println("Test 1: Sending simple message...");
    //         kafkaProducer.sendMessage("test-topic", "testKey", "Hello Kafka!");

    //         // Test 2: Send an Order
    //         System.out.println("\nTest 2: Sending order...");
    //         Order testOrder = new Order(
    //             UUID.randomUUID(),
    //             "AAPL",
    //             150.00,
    //             100,
    //             OrderSide.BUY,
    //             OrderType.LIMIT,
    //             150.00,
    //             Timestamp.from(Instant.now())
    //         );
    //         kafkaProducer.sendOrder(testOrder);

    //         // Wait for a moment to see the results
    //         Thread.sleep(2000);

    //         // Flush the producer
    //         System.out.println("\nFlushing producer...");
    //         kafkaProducer.flushProducer();

    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     } finally {
    //         // Close the Spring context
    //         context.close();
    //     }
    // }
// }