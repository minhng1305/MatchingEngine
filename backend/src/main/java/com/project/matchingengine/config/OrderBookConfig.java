package com.project.matchingengine.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.project.matchingengine.models.order.OrderBook;
import com.project.matchingengine.models.order.Stock;
import com.project.matchingengine.service.user.UserDetailsCacheService;


@Configuration
@Profile("!ingress")
public class OrderBookConfig {
    private static final Logger logger = LoggerFactory.getLogger(OrderBookConfig.class);
    private Map<String, OrderBook> orderBooks;
    private final UserDetailsCacheService userDetailsCacheService;
    private final String[] symbols = Arrays.stream(Stock.values())
                                            .map(Enum::name)
                                            .toArray(String[]::new);

    public OrderBookConfig(UserDetailsCacheService userDetailsCacheService)
    {
        this.orderBooks = new HashMap<>();
        this.userDetailsCacheService = userDetailsCacheService;
        for (String symbol : symbols) {
            orderBooks.put(symbol, new OrderBook(
                    symbol,
                    userDetailsCacheService
            ));
        }
    }

    @Bean
    public Map<String, OrderBook> getOrderBooksMap() {
        return orderBooks;
    }

    public OrderBook getOrderBook(String symbol) {
        if  (!orderBooks.containsKey(symbol)) {
            logger.info("Symbol: {} - Does NOT exist in OrderBooks map", symbol);
            return null;
        }
        return orderBooks.get(symbol);
    }

}