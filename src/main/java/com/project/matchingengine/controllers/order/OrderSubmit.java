package com.project.matchingengine.controllers.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.service.kafka.KafkaProducer;


@RequestMapping("/api/orders")
@Controller
public class OrderSubmit {
    private static final Logger logger = LoggerFactory.getLogger(OrderSubmit.class);
    private final ObjectMapper objectMapper;
    private final KafkaProducer kafkaProducer;

    @Autowired
    public OrderSubmit(ObjectMapper objectMapper, KafkaProducer kafkaProducer) {
        this.objectMapper = objectMapper;
        this.kafkaProducer = kafkaProducer;
    }

     // This method will now handle messages sent to the "/app/order-submit" destination
    @MessageMapping("/submit-order")
    public void submitOrder(@Payload Order order) {
        try {
            // if (order.getUserId() == null || order.getUserId().isEmpty()) {
            //     logger.error("Order received without a userId. Cannot process.");
            //     return; // Or throw an exception
            // }
            logger.info("Received Order via WebSocket: {}. Sending to Kafka...", order.getOrderId());
            kafkaProducer.sendOrder(order);
            logger.info("=> Sent Order to Kafka: {}", order.getOrderId());
        } catch (Exception e) {
            logger.error("Failed to submit order from WebSocket to Kafka: {}", e.getMessage(), e);
        }
    }
}
