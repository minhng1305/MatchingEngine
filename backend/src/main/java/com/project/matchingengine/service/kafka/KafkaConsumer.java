package com.project.matchingengine.service.kafka;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.matchingengine.config.OrderBookConfig;
import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.models.order.OrderBook;
import com.project.matchingengine.repository.order.OrderRepo;
import com.project.matchingengine.service.websocket.WebSocketNotificationService;

@Service
@Profile("!ingress")
public class KafkaConsumer {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);
    private final ObjectMapper objectMapper;
    private final OrderBookConfig orderBookConfig;
    private final WebSocketNotificationService notificationService;
    private final OrderRepo orderRepo;
    private final Executor dbExecutor;
    private CountDownLatch latch;

    @Autowired 
    public KafkaConsumer(ObjectMapper objectMapper,
                         OrderBookConfig orderBookConfig,
                         WebSocketNotificationService notificationService,
                         OrderRepo orderRepo,
                         @Qualifier("dbExecutor") Executor dbExecutor) {
        this.objectMapper = objectMapper;
        this.orderBookConfig = orderBookConfig;
        this.notificationService = notificationService;
        this.orderRepo = orderRepo;
        this.dbExecutor = dbExecutor;
    }


    @KafkaListener(
            id = "orderSubmissionListener",
            topics = "${app.kafka.topic.orders:orders}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void processOrders(
                            List<String> orderJsonBatch,
                            @Header(value = "kafka_receivedPartitionId", required = false) List<Integer> partitions) 
    {
        try {
            if (partitions != null && !partitions.isEmpty()) {
                Set<Integer> uniquePartitions = new HashSet<>(partitions);
                logger.info("Processing {} orders from partitions: {}", 
                    orderJsonBatch.size(), uniquePartitions);
            }
            
            List<Order> orders = new ArrayList<>();
            for (String orderJson : orderJsonBatch) {
                Order order = objectMapper.readValue(orderJson, Order.class);
                orders.add(order);
            }
            // Async batch save to database (non-blocking)
            CompletableFuture.runAsync(() -> {
                try {
                    orderRepo.saveAll(orders);
                    logger.debug("Successfully saved {} orders to database asynchronously", orders.size());
                } catch (Exception e) {
                    logger.error("Failed to save orders to database asynchronously: {}", e.getMessage(), e);
                    // Consider adding retry logic or dead letter queue here
                }
            }, dbExecutor);

            Map<String, List<Order>> ordersBySymbol = orders.stream()
                    .collect(Collectors.groupingBy(Order::getSymbol));

            for (Map.Entry<String, List<Order>> entry : ordersBySymbol.entrySet()) {
                OrderBook orderBook = orderBookConfig.getOrderBook(entry.getKey());
                orderBook.addOrder(entry.getValue());
                // Broadcast once per symbol batch
                notificationService.broadcastOrderBookUpdate(orderBook.getOrderBookSummary());
            }
        } catch (Exception e) {
            logger.error("Failed to process order batch: {}", e.getMessage(), e);
        }
    }
}