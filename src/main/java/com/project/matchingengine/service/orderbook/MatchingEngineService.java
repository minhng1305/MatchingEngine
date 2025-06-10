// // package com.project.matchingengine.service;

// // import org.springframework.beans.factory.annotation.Autowired;
// // import org.springframework.kafka.core.KafkaTemplate;
// // import org.springframework.stereotype.Service;

// // import com.project.matchingengine.models.order.Order;
// // import com.project.matchingengine.models.order.OrderStatus;
// // import com.project.matchingengine.models.order.Trade;
// // import com.project.matchingengine.service.OrderBook;


// // @Service
// // public class MatchingEngineService {

// //     private OrderBook orderBook;
// //     private KafkaTemplate<String, Trade> kafkaTemplate;

// //     public MatchingEngineService(KafkaTemplate<String, Trade> kafkaTemplate, OrderBook orderBook) {
// //         this.kafkaTemplate = kafkaTemplate;
// //         this.orderBook = orderBook;
// //     }

// //     public void processOrder(Order order) {
// //         orderBook.addOrder(order);
// //         if (order.status == OrderStatus.FILLED || order.status == OrderStatus.PARTIALLY_FILLED) {
// //             Trade trade = new Trade(order);
// //             kafkaTemplate.send("trades", trade);
// //         }
// //     }

// //     public double getCurrentPrice(String symbol) {
// //         return orderBook.getCurrentPrice(symbol);
// //     }
// // }

// package com.project.matchingengine.service.orderbook;

// import java.util.List;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.kafka.annotation.KafkaListener;
// import org.springframework.kafka.core.KafkaTemplate;
// import org.springframework.stereotype.Service;

// import com.project.matchingengine.models.order.Order;
// import com.project.matchingengine.models.order.Trade;

// /**
//  * MatchingEngineService is responsible for consuming new orders from Kafka,
//  * processing them through the OrderBook, and publishing executed trades back to Kafka.
//  */
// @Service
// public class MatchingEngineService {

//     private final OrderBook orderBook; // Injected OrderBook instance
//     private final KafkaTemplate<String, Trade> tradeKafkaTemplate; // KafkaTemplate specifically for Trades

//     // Inject the Kafka topic name for trade executions
//     @Value("${app.kafka.topics.trade-execution}")
//     private String tradeExecutionTopic;

//     /**
//      * Constructor for MatchingEngineService. Spring will automatically inject
//      * the OrderBook and KafkaTemplate<String, Trade> beans.
//      * @param orderBook The OrderBook instance to perform matching.
//      * @param tradeKafkaTemplate The KafkaTemplate for sending trade messages.
//      */
//     public MatchingEngineService(OrderBook orderBook, KafkaTemplate<String, Trade> tradeKafkaTemplate) {
//         this.orderBook = orderBook;
//         this.tradeKafkaTemplate = tradeKafkaTemplate;
//     }

//     /**
//      * Kafka listener method that consumes incoming Order messages from the
//      * order submission topic.
//      * It uses the configured 'orderKafkaListenerContainerFactory' for consumption.
//      * @param order The Order object received from Kafka.
//      */
//     @KafkaListener(topics = "${app.kafka.topics.order-submission}",
//                    groupId = "${spring.kafka.consumer.group-id}",
//                    containerFactory = "orderKafkaListenerContainerFactory") // Refers to the bean in KafkaConfig
//     public void processOrder(Order order) {
//         System.out.println("Processing order from Kafka: " + order.getOrderId() + " for symbol " + order.getSymbol());
        
//         // Capture the current number of trades in the order book before adding the new order.
//         // This allows us to identify only the newly generated trades.
//         int initialTradeCount = orderBook.getTrades().size();

//         // Add the order to the order book. This method contains the matching logic
//         // and will update the internal list of trades.
//         orderBook.addOrder(order);

//         // After adding the order, check if any new trades were generated.
//         List<Trade> allTrades = orderBook.getTrades();
//         if (allTrades.size() > initialTradeCount) {
//             // Extract the newly generated trades.
//             // We assume trades are only appended to the list.
//             List<Trade> newTrades = allTrades.subList(initialTradeCount, allTrades.size());
//             for (Trade trade : newTrades) {
//                 System.out.println("Publishing new trade to Kafka: Symbol=" + trade.symbol + ", Quantity=" + trade.quantity + ", Price=" + trade.price);
//                 // Publish each new trade to the trade execution topic.
//                 // Using trade.symbol as the key for partitioning trades.
//                 tradeKafkaTemplate.send(tradeExecutionTopic, trade.symbol, trade);
//             }
//         }
//     }

//     /**
//      * Provides access to the underlying OrderBook instance.
//      * This can be useful for other services or controllers to query the current state
//      * of the order book (e.g., current price, depth).
//      * @return The OrderBook instance.
//      */
//     public OrderBook getOrderBook() {
//         return this.orderBook;
//     }
// }
