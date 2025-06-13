package com.project.matchingengine.controllers.order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.service.kafka.KafkaProducer;


@RequestMapping("/api/orders")
@RestController
public class OrderSubmit {
    private final ObjectMapper objectMapper;
    private final KafkaProducer kafkaProducer;

    @Autowired
    public OrderSubmit(ObjectMapper objectMapper, KafkaProducer kafkaProducer) {
        this.objectMapper = objectMapper;
        this.kafkaProducer = kafkaProducer;
    }

    @PostMapping("/order-submit")
    public ResponseEntity<String> submitOrder(Order order) {
        try {
            kafkaProducer.sendOrder(order);
            return ResponseEntity.ok("Order submitted to Kafka successfully! Order ID: " + order.getOrderId().toString());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to submit order: " + e.getMessage());
        }
    }
}
