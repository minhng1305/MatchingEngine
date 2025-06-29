package com.project.matchingengine.service.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.project.matchingengine.models.order.OrderBookSummary;

@Service
public class WebSocketNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketNotificationService.class);
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public WebSocketNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    
    // public void sendTradeNotificationToUser(Trade trade) {
    //     if (trade.getUserId() == null) {
    //         logger.warn("Cannot send trade notification because userId is null. Trade ID: {}", trade.getTradeId());
    //         return;
    //     }
    //     logger.info("Sending trade confirmation for trade ID {} to user {}", trade.getTradeId(), trade.getUserId());
    //     // The destination "/queue/trades" is automatically prefixed with "/user/{userId}" by Spring
    //     messagingTemplate.convertAndSendToUser(trade.getUserId(), "/queue/trades", trade);
    // }

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