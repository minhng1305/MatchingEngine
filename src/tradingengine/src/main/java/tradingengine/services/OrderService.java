package tradingengine.services;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import tradingengine.models.Order;
import tradingengine.models.OrderBookSummary;
import tradingengine.models.Trade;

@Service
public class OrderService {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, OrderBook> orderBooks;

    public OrderService(KafkaTemplate<String, String> kafkaTemplate, RedisTemplate<String, Object> redisTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.orderBooks = new ConcurrentHashMap<>();
    }

    @KafkaListener(topics = "incoming-orders", groupId = "trading-engine-group")
    public void processOrder(String orderJson) {
        try {
            Order order = objectMapper.readValue(orderJson, Order.class);
            
            // Get or create order book for symbol
            OrderBook orderBook = orderBooks.computeIfAbsent(order.getSymbol(), symbol -> {
                OrderBook newBook = new OrderBook();
                newBook.setSymbol(symbol);
                return newBook;
            });

            // Process order and get trades
            List<Trade> trades = orderBook.addOrder(order);

            // Store order book summary in Redis
            OrderBookSummary summary = orderBook.getOrderBookSummary();
            redisTemplate.opsForValue().set(
                "orderbook:" + order.getSymbol(),
                objectMapper.writeValueAsString(summary)
            );

            // Publish trades to Kafka
            for (Trade trade : trades) {
                String tradeJson = objectMapper.writeValueAsString(trade);
                kafkaTemplate.send("executed-trades", tradeJson);
            }

        } catch (Exception e) {
            // Handle error
            kafkaTemplate.send("error-events", "Error processing order: " + e.getMessage());
        }
    }
}

