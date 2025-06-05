package com.project.matchingengine.tests;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.models.order.OrderSide;
import com.project.matchingengine.models.order.OrderType;
import com.project.matchingengine.service.kafka.KafkaProducer;

@SpringBootApplication
public class KafkaProducerDemo {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(KafkaProducerDemo.class, args);
        
        try {
            // Get the KafkaProducer bean
            KafkaProducer kafkaProducer = context.getBean(KafkaProducer.class);

            // Test 1: Send a simple message
            System.out.println("Test 1: Sending simple message...");
            kafkaProducer.sendMessage("test-topic", "testKey", "Hello Kafka!");

            // Test 2: Send an Order
            System.out.println("\nTest 2: Sending order...");
            Order testOrder = new Order(
                UUID.randomUUID(),
                "AAPL",
                150.00,
                100,
                OrderSide.BUY,
                OrderType.LIMIT,
                150.00,
                Timestamp.from(Instant.now())
            );
            kafkaProducer.sendOrder(testOrder);

            // Wait for a moment to see the results
            Thread.sleep(2000);

            // Flush the producer
            System.out.println("\nFlushing producer...");
            kafkaProducer.flushProducer();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Close the Spring context
            context.close();
        }
    }
}