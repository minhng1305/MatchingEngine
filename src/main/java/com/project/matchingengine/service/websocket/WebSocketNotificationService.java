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
// import com.project.matchingengine.models.order.Trade;

@Service
public class WebSocketNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketNotificationService.class);
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    public WebSocketNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Send order update to specific user
     */
    public void sendOrderUpdate(String userId, Order order) {
        try {
            String message = objectMapper.writeValueAsString(order);
            messagingTemplate.convertAndSendToUser(userId, "/queue/orders", message);
            logger.info("Sent order update to user {}: {}", userId, order.getOrderId().toString());
        } catch (JsonProcessingException e) {
            logger.error("Error serializing order update", e);
        }
    }
    
    /**
     * Send trade notification to all subscribers of a symbol
     */
    // public void sendTradeNotification(Trade trade) {
    //     try {
    //         String message = objectMapper.writeValueAsString(trade);
    //         messagingTemplate.convertAndSend("/topic/trades/" + trade.getSymbol(), message);
            
    //         // Also send to specific users involved in the trade
    //         messagingTemplate.convertAndSendToUser(trade., "/queue/trades", message);
    //         messagingTemplate.convertAndSendToUser(trade.getSellUserId(), "/queue/trades", message);
            
    //         logger.info("Sent trade notification for symbol {}: {}", trade.getSymbol(), trade.getTradeId());
    //     } catch (JsonProcessingException e) {
    //         logger.error("Error serializing trade notification", e);
    //     }
    // }

    /**
     * Send order book update to all subscribers of a symbol
     */
    public void sendOrderBookUpdate(OrderBookSummary orderBookSummary) {
        try {
            String message = objectMapper.writeValueAsString(orderBookSummary);
            messagingTemplate.convertAndSend("/topic/orderbook/" + orderBookSummary.symbol, message);
            logger.info("Sent order book update for symbol {}", orderBookSummary.symbol);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing order book update", e);
        }
    }

    /**
     * Send market data update (aggregated data)
     */
    public void sendMarketDataUpdate(String symbol, Object marketData) {
        try {
            String message = objectMapper.writeValueAsString(marketData);
            messagingTemplate.convertAndSend("/topic/market/" + symbol, message);
            logger.info("Sent market data update for symbol {}", symbol);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing market data update", e);
        }
    }


    /**
     * Send system notification to all connected clients
     */
    public void sendSystemNotification(String notification) {
        messagingTemplate.convertAndSend("/topic/system", notification);
        logger.info("Sent system notification: {}", notification);
    }


    /**
     * Broadcasts the updated order book summary to all subscribed clients.
     * Clients must be subscribed to /topic/market-data.
     * @param summary The order book summary.
     */
    public void broadcastOrderBookUpdate(OrderBookSummary summary) {
        logger.info("Broadcasting market data update for symbol {}", summary.symbol);
        messagingTemplate.convertAndSend("/topic/market-data", summary);
    }
}