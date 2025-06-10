// package com.project.matchingengine.controllers.order;

// import com.project.matchingengine.models.order.OrderBookSummary;
// import com.project.matchingengine.service.orderbook.OrderBook;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.kafka.core.KafkaTemplate;
// import org.springframework.stereotype.Controller;


// @Controller
// public class MarketDataUpdate {
//     private final OrderBook orderBook; 
//     private final KafkaTemplate<String, OrderBookSummary> marketDataKafkaTemplate;

//     @Value("${app.kafka.topics.market-data-updates}")
//     private String marketDataUpdatesTopic;

//     public MarketDataService(OrderBook orderBook, KafkaTemplate<String, OrderBookSummary> marketDataKafkaTemplate) {
//         this.orderBook = orderBook;
//         this.marketDataKafkaTemplate = marketDataKafkaTemplate;
//     }

//     /**
//      * Publishes a snapshot of the current market data for a given symbol to Kafka.
//      * This method should be called by the MatchingEngineService whenever the order book
//      * state changes significantly (e.g., after an order is added or a trade occurs).
//      * @param symbol The trading symbol for which to publish market data.
//      */
//     public void publishMarketDataUpdate() {
//         OrderBookSummary summary = orderBook.getOrderBookSummary();

//         // Publish the summary to the market data updates topic, using symbol as key
//         marketDataKafkaTemplate.send(marketDataUpdatesTopic, orderBook.symbol, summary);
//     }
// }