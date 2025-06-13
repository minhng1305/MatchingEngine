// package com.project.matchingengine.service.orderbook;

// import java.util.List;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.kafka.annotation.KafkaListener;
// import org.springframework.kafka.core.KafkaTemplate;
// import org.springframework.stereotype.Service;

// import com.project.matchingengine.models.order.Order;
// import com.project.matchingengine.models.order.Trade;


// @Service
// public class MatchingEngineService {

//     private final OrderBook orderBook; 
//     private final KafkaTemplate<String, Trade> kafkaTemplate; 

//     @Value("${app.kafka.topics.trade-execution}")
//     private String tradeExecutionTopic;


//     public MatchingEngineService(OrderBook orderBook, KafkaTemplate<String, Trade> kafkaTemplate) {
//         this.orderBook = orderBook;
//         this.kafkaTemplate = kafkaTemplate;
//     }


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
//                 kafkaTemplate.send(tradeExecutionTopic, trade.symbol, trade);
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
