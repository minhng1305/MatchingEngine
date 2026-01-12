package com.project.matchingengine.service.kafka;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.matchingengine.config.OrderBookConfig;
import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.models.order.OrderBook;
import com.project.matchingengine.service.websocket.WebSocketNotificationService;
import com.project.matchingengine.repository.order.OrderRepo;

@Service
public class KafkaConsumer {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);
    private final ObjectMapper objectMapper;
    private final OrderBookConfig orderBookConfig;
    private final WebSocketNotificationService notificationService;
    private final OrderRepo orderRepo;
    private CountDownLatch latch;

    @Autowired 
    public KafkaConsumer(ObjectMapper objectMapper,
                         OrderBookConfig orderBookConfig,
                         WebSocketNotificationService notificationService,
                         OrderRepo orderRepo) {
        this.objectMapper = objectMapper;
        this.orderBookConfig = orderBookConfig;
        this.notificationService = notificationService;
        this.orderRepo = orderRepo;
    }

    public void setLatch(CountDownLatch latch) {
        this.latch = latch;
    }
    
    @KafkaListener(
            id = "orderSubmissionListener",
            topics = "#{@topicListProvider.getTopics()}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void processOrder(String orderJson) {
        try {
            Order order = objectMapper.readValue(orderJson, Order.class);

            logger.info("Processing order from symbol-specific topic: {} for symbol: {}",
                    order.getOrderId(), order.getSymbol());

            // Save order to database first (with initial status) before processing
            // This ensures orders exist when trades reference them
            orderRepo.save(order);
            logger.debug("Order {} saved to database with status: {}", order.getOrderId(), order.getStatus());

            OrderBook orderBook = orderBookConfig.getOrderBook(order.getSymbol());

            if (latch != null) {
                latch.countDown();
            }
            orderBook.addOrder(order);

            logger.info("Current Price for {}: {}", order.getSymbol(), orderBook.getCurrentPrice());

            notificationService.broadcastOrderBookUpdate(orderBook.getOrderBookSummary());
        } catch (Exception e) {
            logger.error("Failed to process order from Kafka: {}", e.getMessage(), e);
        }
    }
}