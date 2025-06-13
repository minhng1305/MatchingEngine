package com.project.matchingengine.controllers.order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.matchingengine.config.OrderBookConfig;
import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.service.orderbook.OrderBook;


@RequestMapping("/trade-execution")
@RestController
public class TradeExecute {
    private final ObjectMapper objectMapper;
    private final OrderBookConfig orderBookConfig;

    @Autowired
    public TradeExecute(ObjectMapper objectMapper, 
                        OrderBookConfig orderBookConfig) {
        this.objectMapper = objectMapper;
        this.orderBookConfig = orderBookConfig;
    }

    @PostMapping("/matching-order")
    public ResponseEntity<String> matchingOrder(@RequestBody Order order) {
        try {
            OrderBook orderBook = orderBookConfig.getOrCreateOrderBook(order.getSymbol());
            orderBook.addOrder(order);
            return ResponseEntity.ok("Order matched successfully! Order ID: " + order.getOrderId().toString());
            // Optionally, you can publish market data updates here           
             
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to submit order: " + e.getMessage());
        }
    }


}