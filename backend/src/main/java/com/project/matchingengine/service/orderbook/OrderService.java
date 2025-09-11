package com.project.matchingengine.service.orderbook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.service.kafka.KafkaProducer;

@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private final KafkaProducer kafkaProducer;

    @Autowired
    public OrderService(KafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    public void submitOrder(Order order) {
        kafkaProducer.sendOrder(order);
        logger.info("Order submitted to Kafka: {}", order.getOrderId());
    }
}
