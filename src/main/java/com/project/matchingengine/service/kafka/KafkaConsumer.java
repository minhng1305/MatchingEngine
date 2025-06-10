package com.project.matchingengine.service.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.matchingengine.models.order.Order;





@Service
public class KafkaConsumer {

    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);

    // @Value("${app.kafka.topics.order-submission}")
    // private static final String orderSubmissionTopic;

    // @Value("${spring.kafka.consumer.group-id}")
    // private static final String consumerGroupId;

    public KafkaConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "test", groupId = "myGroup")
    public void listen(String message) {
        logger.warn("This is a Secret message " + message);
    }
    
    @KafkaListener( id = "orderSubmissionListener", topics = "${app.kafka.topics.order-submission}", groupId = "${spring.kafka.consumer.group-id}" )
    public void processOrder(String orderJson) {
        // Logic to consume the order from Kafka
        // This could involve deserializing the order, processing it, etc.);
        try {
            Order order = objectMapper.readValue(orderJson, Order.class);//
            System.out.println("Received Order: " + order.getOrderId());
        } catch (Exception e) {
            logger.error("Failed to process order from Kafka: {}", e.getMessage(), e);
        }

    }

    // @KafkaListener(topics = "test-topic", groupId = "myGroup")
    // public void listen(String message) {
    //     System.out.println("Received message: " + message);
    //     logger.warn("This is a Secret message " + message);
    // }
}