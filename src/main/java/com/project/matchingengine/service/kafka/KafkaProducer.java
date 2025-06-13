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


    // public void sendOrderBookSummary(OrderBookSummary orderBookSummary) {
    //     try {
    //         String summaryJson = objectMapper.writeValueAsString(orderBookSummary);

    //         CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(marketDataUpdatesTopic, orderBookSummary.symbol, summaryJson);
            
    //         future.whenComplete((result, ex) -> {
    //             if (ex == null) {
    //                 logger.info("Asynchronous Send (SUCCESS): Order {} sent to partition {} at offset {}",
    //                         orderBookSummary.symbol,
    //                         result.getRecordMetadata().partition(),
    //                         result.getRecordMetadata().offset());
    //             } else {
    //                 logger.error("Asynchronous Send (FAILURE): Failed to send order {}: {}",
    //                             orderBookSummary.symbol, ex.getMessage(), ex);
    //             }
    //         });
    //         logger.info("Order book summary sent for symbol: {}", orderBookSummary.symbol);
            
    //     } catch (JsonProcessingException e) {
    //         logger.error("Failed to serialize order book summary", e);
    //         throw new RuntimeException("Failed to send order book summary", e);
    //     }
    // }

    // public void sendMessage(String topic, String key, String value) {
    //     try {
    //         System.out.println("Sending message to Kafka topic: " + topic + " with key: " + key + " and value: " + value);
    //         kafkaTemplate.send(topic, key, value);
    //         System.out.println("Message sent successfully to Kafka topic: " + topic);
    //     } catch (Exception e) {
    //         throw new RuntimeException("Failed to send message to Kafka", e);
    //     }
    // }

    public void flushProducer() {
        kafkaTemplate.flush();
        logger.info("Kafka Producer flushed.");
    }
}