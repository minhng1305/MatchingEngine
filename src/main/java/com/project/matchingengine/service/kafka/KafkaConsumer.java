package com.project.matchingengine.service.kafka;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.matchingengine.config.OrderBookConfig;
import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.models.order.Trade;
import com.project.matchingengine.service.orderbook.OrderBook;
import com.project.matchingengine.service.websocket.WebSocketNotificationService;

@Service
public class KafkaConsumer {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);
    private final ObjectMapper objectMapper;
    private final OrderBookConfig orderBookConfig;
    private final WebSocketNotificationService notificationService;
    private CountDownLatch latch;

    @Autowired 
    public KafkaConsumer(ObjectMapper objectMapper,
                         OrderBookConfig orderBookConfig,
                         WebSocketNotificationService notificationService) {
        this.objectMapper = objectMapper;
        this.orderBookConfig = orderBookConfig;
        this.notificationService = notificationService;
    }

    public void setLatch(CountDownLatch latch) {
        this.latch = latch;
    }
    
    @KafkaListener( id = "orderSubmissionListener", topics = "${app.kafka.topics.order-submission}", groupId = "${spring.kafka.consumer.group-id}" )
    public void processOrder(String orderJson) {
        try {
            Order order = objectMapper.readValue(orderJson, Order.class);
            // logger.info("Received Order: {}", order.getOrderId());
            // pass the order to oderboook
            OrderBook orderBook = orderBookConfig.getOrCreateOrderBook(order.getSymbol());
            
            // if (order.status == OrderStatus.PARTIALLY_FILLED ||order.status == OrderStatus.FILLED ){
            //     notificationService.sendTradeNotificationToUser(trade);
            // }

            // can be deleted later
            if (latch != null) {
                latch.countDown();
            }

            orderBook.addOrder(order);
            logger.info("Price: {}", orderBook.getCurrentPrice());
            notificationService.broadcastOrderBookUpdate(orderBook.getOrderBookSummary());
        } catch (Exception e) {
            logger.error("Failed to process order from Kafka: {}", e.getMessage(), e);
        }
    }

    private static void printTrades(ArrayList<Trade> trades) {
        if (trades.isEmpty()) {
            System.out.println("No trades executed");
            return;
        }
        
        for (int i = 0; i < trades.size(); i++) {
            Trade trade = trades.get(i);
            // logger.info("Trade " + (i + 1) + ": " + trade.symbol + 
            //                    " || Price: " + trade.price + 
            //                    " || Quantity: " + trade.quantity + 
            //                    " || Buy Order: " + trade.getBuyOrderId() + 
            //                    " || Sell Order: " + trade.getSellOrderId() +
            //                    " || Timestamp: " + trade.tradeTimestamp);

            logger.info("Trade {}: {} || Price: {} || Quantity: {} || Buy Order: {} || Sell Order: {} || Timestamp: {}",
                    i + 1, trade.symbol, trade.price, trade.quantity, 
                    trade.getBuyOrderId(), trade.getSellOrderId(), trade.tradeTimestamp);
        }
    }
}