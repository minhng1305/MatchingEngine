package tradingengine;

import redis.clients.jedis.Jedis;
import tradingengine.models.Order;
import tradingengine.models.OrderBookSummary;
import tradingengine.models.Trade;
import tradingengine.services.OrderBook;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Duration;

public class MatchingEngineApplication {
    private final Jedis jedis;
    private final KafkaProducer<String, String> producer;
    private final KafkaConsumer<String, String> consumer;
    private final ObjectMapper objectMapper;
    private final Map<String, OrderBook> orderBooks;


    public MatchingEngineApplication(Jedis jedis, KafkaProducer<String, String> producer, KafkaConsumer<String, String> consumer) {
        this.jedis = jedis;
        this.producer = producer;
        this.consumer = consumer;
        this.objectMapper = new ObjectMapper();
        this.orderBooks = new ConcurrentHashMap<>();
    }

    public static void main(String[] args) {
        Properties kafkaProps = new Properties();
        // Producer properties
        kafkaProps.put("bootstrap.servers", "localhost:9092");
        kafkaProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProps.put("acks", "all");
        
        // Consumer properties
        Properties consumerProps = new Properties();
        consumerProps.put("bootstrap.servers", "localhost:9092");
        consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("group.id", "matching-engine-group");
        consumerProps.put("auto.offset.reset", "earliest");
        consumerProps.put("enable.auto.commit", "true");
        
        KafkaProducer<String, String> producer = new KafkaProducer<>(kafkaProps);
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        Jedis jedis = new Jedis("localhost", 6379);
        
        MatchingEngineApplication engineApplication = new MatchingEngineApplication(jedis, producer, consumer);
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down matching engine...");
            producer.close();
            consumer.close();
            jedis.close();
        }));

        engineApplication.start();
    }


    public void start() {
        consumer.subscribe(Arrays.asList("orders"));
        System.out.println("Matching Engine started and waiting for orders...");
        
        while (true) {
            try {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    try {
                        Order order = deserializeOrder(record.value());
                        processOrder(order);
                    } catch (Exception e) {
                        System.err.println("Error processing order: " + e.getMessage());
                        sendErrorMessage("Error processing order: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.err.println("Error polling messages: " + e.getMessage());
            }
        }
    }

    private Order deserializeOrder(String orderJson) {
        try {
            return objectMapper.readValue(orderJson, Order.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize order: " + e.getMessage());
        }
    }

    private void processOrder(Order order) {
        // Get or create order book for symbol
        OrderBook orderBook = orderBooks.computeIfAbsent(order.getSymbol(), symbol -> {
            OrderBook newBook = new OrderBook();
            newBook.setSymbol(symbol);
            return newBook;
        });

        // Process order and get trades
        List<Trade> trades = orderBook.addOrder(order);

        // Store order book summary in Redis
        try {
            OrderBookSummary summary = orderBook.getOrderBookSummary();
            String summaryJson = objectMapper.writeValueAsString(summary);
            jedis.set("orderbook:" + order.getSymbol(), summaryJson);

            // Publish trades to Kafka
            for (Trade trade : trades) {
                String tradeJson = objectMapper.writeValueAsString(trade);
                producer.send(new ProducerRecord<>("executed-trades", tradeJson));
            }

            // Print status
            System.out.printf("Processed order %s: %s %s %d@%d\n",
                order.getOrderId(),
                order.getSymbol(),
                order.side,
                order.getOriginalQuantity(),
                order.getPrice());

        } catch (Exception e) {
            throw new RuntimeException("Failed to process order: " + e.getMessage());
        }
    }

    private void sendErrorMessage(String errorMessage) {
        try {
            producer.send(new ProducerRecord<>("error-events", errorMessage));
        } catch (Exception e) {
            System.err.println("Failed to send error message to Kafka: " + e.getMessage());
        }
    }
}
