package tradingengine;

import tradingengine.models.Order;
import tradingengine.models.Order.OrderType;

import java.sql.Timestamp;
import java.util.Random;
import java.util.UUID;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TradingEngineTest {
    private static final String[] SYMBOLS = {"AAPL", "GOOGL", "MSFT", "AMZN"};
    private static final Random random = new Random();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        // Configure Kafka producer
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("acks", "all");

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        // Start the matching engine in a separate thread
        Thread engineThread = new Thread(() -> {
            MatchingEngineApplication.main(new String[]{});
        });
        engineThread.start();

        // Generate and send synthetic orders
        for (int i = 0; i < 20; i++) {
            Order order = generateRandomOrder();
            String orderJson = objectMapper.writeValueAsString(order);
            producer.send(new ProducerRecord<>("orders", orderJson));
            
            System.out.println("Sent order: " + 
                order.getSymbol() + " " + 
                order.side + " " + 
                order.getOriginalQuantity() + "@" + 
                order.getPrice());
            
            Thread.sleep(1000); // Wait 1 second between orders
        }

        producer.close();
    }

    private static Order generateRandomOrder() {
        String symbol = SYMBOLS[random.nextInt(SYMBOLS.length)];
        String side = random.nextBoolean() ? "BUY" : "SELL";
        OrderType orderType = OrderType.LIMIT; // Using LIMIT orders for testing
        
        // Generate price between 100 and 200
        long price = 100 + random.nextInt(101);
        
        // Generate quantity between 1 and 100
        int quantity = 1 + random.nextInt(100);
        
        return new Order(
            UUID.randomUUID(),
            symbol,
            "user" + random.nextInt(10), // Random user ID
            side,
            orderType,
            price,
            quantity,
            new Timestamp(System.currentTimeMillis())
        );
    }
}