package com.project.matchingengine.service.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.matchingengine.models.order.Order;


@Service
public class KafkaProducer {
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducer.class);
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.topics.order-submission-prefix:order-}")
    private String orderTopicPrefix;


    public KafkaProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }


    public void sendOrder(Order order) {
        try {
            String orderJson = objectMapper.writeValueAsString(order);
            String topicName = generateTopicName(order.getSymbol());

            kafkaTemplate.send(topicName, order.getOrderId().toString(), orderJson);
            logger.info("Order {} sent to topic: {}", order.getOrderId().toString(), topicName);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize order to JSON", e);
        }
    }

    private String generateTopicName(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Order symbol cannot be null or empty");
        }
        String cleanSymbol = symbol.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        return orderTopicPrefix + cleanSymbol;
    }


    public void flushProducer() {
        kafkaTemplate.flush();
        logger.info("Kafka Producer flushed.");
    }
}