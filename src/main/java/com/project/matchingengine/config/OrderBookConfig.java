package com.project.matchingengine.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.project.matchingengine.service.orderbook.OrderBook;


@Configuration
public class OrderBookConfig {
    private Map<String, OrderBook> orderBooks;
    private final String[] symbols = {"AAPL", "GOOGL", "AMZN", "MSFT", "TSLA"};

    public OrderBookConfig() {
        this.orderBooks = new HashMap<>();
        for (String symbol : symbols) {
            orderBooks.put(symbol, new OrderBook(symbol));
        }
    }

    @Bean
    public Map<String, OrderBook> getOrderBooksMap() {
        return orderBooks;
    }

    public OrderBook getOrCreateOrderBook(String symbol) {
        if  (!orderBooks.containsKey(symbol)) {
            orderBooks.put(symbol, new OrderBook(symbol));
        }
        return orderBooks.get(symbol);
    }

}