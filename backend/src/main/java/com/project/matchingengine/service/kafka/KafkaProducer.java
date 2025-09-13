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

    @Value("${app.kafka.topics.order-submission}")
    private String orderSubmissionTopic;

    @Value("${app.kafka.topics.market-data-updates}")
    private String marketDataUpdatesTopic;


    public KafkaProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }


    public void sendOrder(Order order) {
        try {
            logger.info("Attempting Asynchronous send for Order: {}", order.getOrderId().toString());
            String orderJson = objectMapper.writeValueAsString(order);

            // Synchronous send (for demonstration purposes, can be removed in production)
            kafkaTemplate.send(orderSubmissionTopic, order.getOrderId().toString(), orderJson);

            // Asynchronous send with CompletableFuture
            // CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(orderSubmissionTopic, order.getOrderId().toString(), orderJson);

            // future.whenComplete((result, ex) -> {
            //     if (ex == null) {
            //         logger.info("Asynchronous Send (SUCCESS): Order {} sent to partition {} at offset {}",
            //                 order.getOrderId().toString(),
            //                 result.getRecordMetadata().partition(),
            //                 result.getRecordMetadata().offset());
            //     } else {
            //         logger.error("Asynchronous Send (FAILURE): Failed to send order {}: {}",
            //                 order.getOrderId().toString(), ex.getMessage(), ex);
            //     }
            // });
            // logger.info("Asynchronous Send: Initiated sending order {}. Continuing without waiting...", order.getOrderId());

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize order to JSON", e);
        }
    }

    public void flushProducer() {
        kafkaTemplate.flush();
        logger.info("Kafka Producer flushed.");
    }
}