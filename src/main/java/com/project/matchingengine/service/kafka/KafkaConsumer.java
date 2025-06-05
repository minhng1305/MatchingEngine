package com.project.matchingengine.service.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.matchingengine.models.order.Order;


@Service
public class KafkaConsumer {

    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);


    public KafkaConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    public void consumeOrder(Order order) {
        // Logic to consume the order from Kafka
        // This could involve deserializing the order, processing it, etc.
        System.out.println("Consuming order: " + order.getOrderId());
        // Further processing logic goes here
    }

    // @KafkaListener(topics = "test-topic", groupId = "myGroup")
    // public void listen(String message) {
    //     System.out.println("Received message: " + message);
    //     logger.warn("This is a Secret message " + message);
    // }
}