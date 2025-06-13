package com.project.matchingengine;


// import org.springframework.boot.SpringApplication;
// import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication
// public class Application {
//     public static void main(String[] args) {
//         SpringApplication.run(Application.class, args);
//     }
// }

import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.models.order.OrderSide;
import com.project.matchingengine.models.order.OrderType;
import com.project.matchingengine.service.kafka.KafkaConsumer;
import com.project.matchingengine.service.kafka.KafkaProducer;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.project.matchingengine.config",
    "com.project.matchingengine.service.kafka", 
    "com.project.matchingengine"
})
public class Application {
    
    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
        
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
    public CommandLineRunner demoKafka(KafkaProducer kafkaProducer, KafkaConsumer orderConsumer) {
        System.out.println("Starting Kafka Producer Demo...");
        logger.info("Starting Kafka Producer Demo...");
        return args -> {
            logger.info("====== STARTING KAFKA CONSUMER DEMO ======");
            logger.info("This demo will produce two orders and consume them from Kafka");
            
            // Set up a latch to wait for messages to be consumed
            CountDownLatch latch = new CountDownLatch(2);
            orderConsumer.setLatch(latch);
            
            // Create and send sample order
            Order buyOrder1 = createOrder("AAPL", 150.0, 100, OrderSide.BUY);
            Order sellOrder1 = createOrder("AAPL", 148.0, 50, OrderSide.SELL);
            Order buyOrder2 = createOrder("AAPL", 149.5, 200, OrderSide.BUY);
            Order sellOrder2 = createOrder("AAPL", 149.0, 150, OrderSide.SELL);
            
            logger.info("Sending BUY order: {}", buyOrder1.getOrderId());
            kafkaProducer.sendOrder(buyOrder1);
            
            logger.info("Sending SELL order: {}", sellOrder1.getOrderId());
            kafkaProducer.sendOrder(sellOrder1);

            logger.info("Sending BUY order: {}", buyOrder2.getOrderId());
            kafkaProducer.sendOrder(buyOrder2);
            
            logger.info("Sending SELL order: {}", sellOrder2.getOrderId());
            kafkaProducer.sendOrder(sellOrder2);
            
            kafkaProducer.flushProducer();
            logger.info("Orders sent to Kafka. Waiting for consumer to process them...");
            
            // Wait for the consumer to receive both messages
            boolean receivedAllMessages = latch.await(15, TimeUnit.SECONDS);
            
            if (receivedAllMessages) {
                logger.info("====== DEMO COMPLETED SUCCESSFULLY ======");
                logger.info("Successfully produced and consumed 4 orders through Kafka!");
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
    
    // @Component
    // public static class OrderConsumerDemo {
    //     private final Logger logger = LoggerFactory.getLogger(OrderConsumerDemo.class);
    //     private final ObjectMapper objectMapper;
    //     private CountDownLatch latch;
        
    //     @Autowired
    //     public OrderConsumerDemo(ObjectMapper objectMapper) {
    //         this.objectMapper = objectMapper;
    //     }
        
    //     public void setLatch(CountDownLatch latch) {
    //         this.latch = latch;
    //     }
        
    //     @KafkaListener(id = "orderSubmissionListener_test", topics = "${app.kafka.topics.order-submission}", groupId = "${spring.kafka.consumer.group-id}")
    //     public void consumeOrder(String orderJson) {
    //         try {
    //             Order order = objectMapper.readValue(orderJson, Order.class);
    //             logger.info("==> CONSUMER: Received order: {}, side: {}, symbol: {}, price: {}, quantity: {}",
    //                     order.getOrderId(),
    //                     order.getSide(),
    //                     order.getSymbol(),
    //                     order.getPrice(),
    //                     order.getOriginalQuantity());
                
    //             // Count down the latch to signal message received
    //             if (latch != null) {
    //                 latch.countDown();
    //                 logger.info("   Remaining orders to receive: {}", latch.getCount());
    //             }
    //         } catch (Exception e) {
    //             logger.error("Failed to process order: {}", e.getMessage(), e);
    //         }
    //     }
    // }
}
