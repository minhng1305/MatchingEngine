package com.project.matchingengine.service.kafka;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.matchingengine.config.OrderBookConfig;
import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.models.order.Trade;
import com.project.matchingengine.service.orderbook.OrderBook;

@Service
public class KafkaConsumer {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);
    private final ObjectMapper objectMapper;
    private final OrderBookConfig orderBookConfig;

    public KafkaConsumer(ObjectMapper objectMapper,
                         OrderBookConfig orderBookConfig) {
        this.objectMapper = objectMapper;
        this.orderBookConfig = orderBookConfig;
    }
    
    @KafkaListener( id = "orderSubmissionListener", topics = "${app.kafka.topics.order-submission}", groupId = "${spring.kafka.consumer.group-id}" )
    public void processOrder(String orderJson) {
        try {
            Order order = objectMapper.readValue(orderJson, Order.class);
            logger.info("Received Order: {}", order.getOrderId());
            // pass the order to oderboook
            OrderBook orderBook = orderBookConfig.getOrCreateOrderBook(order.getSymbol());
            

            // can be deleted later
            logger.info("Processing Order: {} ...", order.getOrderId());
            orderBook.addOrder(order);
            logger.info("Processed order: {}", order.getOrderId());

            printTrades(orderBook.getTrades());
            logger.info("Price: {}", orderBook.getCurrentPrice());

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