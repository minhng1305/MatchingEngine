package com.project.matchingengine.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.project.matchingengine.service.orderbook.OrderBook;
import com.project.matchingengine.service.orderbook.OrderService;
import com.project.matchingengine.service.orderbook.TradeService;



@Configuration
public class OrderBookConfig {
    private Map<String, OrderBook> orderBooks;
    private final String[] symbols = {"AAPL", "GOOGL", "AMZN", "MSFT", "TSLA"};
    private final OrderService orderService;
    private final TradeService tradeService;

    public OrderBookConfig(OrderService orderService, TradeService tradeService) {
        this.orderService = orderService;
        this.tradeService = tradeService;
        this.orderBooks = new HashMap<>();
        for (String symbol : symbols) {
            orderBooks.put(symbol, new OrderBook(symbol, orderService, tradeService));
        }
    }

    @Bean
    public Map<String, OrderBook> getOrderBooksMap() {
        return orderBooks;
    }

    public OrderBook getOrCreateOrderBook(String symbol) {
        if  (!orderBooks.containsKey(symbol)) {
            orderBooks.put(symbol, new OrderBook(symbol, orderService, tradeService));
        }
        return orderBooks.get(symbol);
    }

}