package com.project.matchingengine.service.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.models.order.OrderBookSummary;


@Service
public class WebSocketNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketNotificationService.class);
    private final SimpMessagingTemplate messagingTemplate;
    private ObjectMapper objectMapper;

    @Autowired
    public WebSocketNotificationService(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
    }


//    public void sendOrderUpdate(String userId, Order order) {
//        try {
//            String message = objectMapper.writeValueAsString(order);
//            messagingTemplate.convertAndSendToUser(userId, "/queue/orders", message);
//            logger.info("Sent order update to user {}: {}", userId, order.getOrderId().toString());
//        } catch (JsonProcessingException e) {
//            logger.error("Error serializing order update", e);
//        }
//    }
//
//
//    public void sendOrderBookUpdate(OrderBookSummary orderBookSummary) {
//        try {
//            String message = objectMapper.writeValueAsString(orderBookSummary);
//            messagingTemplate.convertAndSend("/topic/orderbook/" + orderBookSummary.symbol, message);
//            logger.info("Sent order book update for symbol {}", orderBookSummary.symbol);
//        } catch (JsonProcessingException e) {
//            logger.error("Error serializing order book update", e);
//        }
//    }
//
//    /**
//     * Send market data update (aggregated data)
//     */
//    public void sendMarketDataUpdate(String symbol, Object marketData) {
//        try {
//            String message = objectMapper.writeValueAsString(marketData);
//            messagingTemplate.convertAndSend("/topic/market/" + symbol, message);
//            logger.info("Sent market data update for symbol {}", symbol);
//        } catch (JsonProcessingException e) {
//            logger.error("Error serializing market data update", e);
//        }
//    }


//    /**
//     * Send system notification to all connected clients
//     */
//    public void sendSystemNotification(String notification) {
//        messagingTemplate.convertAndSend("/topic/system", notification);
//        logger.info("Sent system notification: {}", notification);
//    }


    public void broadcastOrderBookUpdate(OrderBookSummary summary) {
        if (summary == null) {
            logger.warn("Attempted to broadcast a null order book summary.");
            return;
        }
        String symbol = summary.symbol;
        String destination = "/topic/orderbook-updates/" + symbol;

        logger.info("Broadcasting order book update for symbol '{}' to destination: {}", symbol, destination);
        messagingTemplate.convertAndSend(destination, summary);
    }


}