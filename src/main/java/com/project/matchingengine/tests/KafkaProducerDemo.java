package com.project.matchingengine.tests;

import java.sql.Timestamp;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.models.order.OrderSide;
import com.project.matchingengine.models.order.OrderType;


public class KafkaProducerDemo {
    private static final Logger log = LoggerFactory.getLogger(KafkaProducerDemo.class);

    public static void main(String[] args) {
        log.info("I am a Kafka Producer");

        String bootstrapServers = "127.0.0.1:9092";

        // create Producer properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // create the producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        // Order buyOrder1 = createOrder("AAPL", 150.0, 100, OrderSide.BUY);
        // Order sellOrder1 = createOrder("AAPL", 148.0, 50, OrderSide.SELL);
        // Order buyOrder2 = createOrder("AAPL", 149.5, 200, OrderSide.BUY);
        // Order sellOrder2 = createOrder("AAPL", 149.0, 150, OrderSide.SELL);

        // System.out.println("Sending order to Kafka topic: " + buyOrder1.getOrderId());
        // ProducerRecord<String, String> producerRecord = new ProducerRecord<>(buyOrder1.getOrderId().toString(), buyOrder1.getSymbol() + "," + buyOrder1.getPrice() + "," + buyOrder1.getOriginalQuantity() + "," + buyOrder1.getSide());    
        // // log.info("Order created: " + buyOrder1.getOrderId() + " || price: " + buyOrder1.getPrice() + " || quantity: " + buyOrder1.getOriginalQuantity());
        // System.out.println("Order created: " + buyOrder1.getOrderId() + " || price: " + buyOrder1.getPrice() + " || quantity: " + buyOrder1.getOriginalQuantity());

        ProducerRecord<String, String> producerRecord = new ProducerRecord<>("demo_java", "hello world");

        // send data - asynchronous
        producer.send(producerRecord);

        // flush data - synchronous
        producer.flush();
        
        // flush and close producer
        producer.close();
    }

    private static Order createOrder(String symbol, double price, int quantity, OrderSide side) {
        return new Order(
            UUID.randomUUID(),
            symbol,
            price,
            quantity,
            side,
            OrderType.LIMIT,
            price,
            new Timestamp(System.currentTimeMillis())
        );
    }
}