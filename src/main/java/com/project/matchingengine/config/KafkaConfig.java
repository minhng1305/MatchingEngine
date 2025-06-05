package com.project.matchingengine.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.project.matchingengine.models.order.Order;
import com.project.matchingengine.models.order.OrderBookSummary;
import com.project.matchingengine.models.order.Trade;



@Configuration
public class KafkaConfig {
    // Inject Kafka bootstrap servers from application.properties
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // Inject Kafka consumer group ID from application.properties
    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroupId;

    // Inject order submission topic name from application.properties
    @Value("${app.kafka.topics.order-submission}")
    private String orderSubmissionTopic;

    // Inject trade execution topic name from application.properties
    @Value("${app.kafka.topics.trade-execution}")
    private String tradeExecutionTopic;

    // NEW: Inject market data updates topic name from application.properties
    @Value("${app.kafka.topics.market-data-updates}")
    private String marketDataUpdatesTopic;


    // @Bean
    // public KafkaTemplate<String, Order> kafkaTemplate() {
    //     return new KafkaTemplate<>(producerFactory());
    // }


    // --- Kafka Producer Configuration for Orders ---

    /**
     * Configures the ProducerFactory for sending Order objects to Kafka.
     * Uses StringSerializer for keys and JsonSerializer for Order values.
     * @return ProducerFactory for Order messages.
     */
    @Bean
    public ProducerFactory<String, Order> orderProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Creates a KafkaTemplate for sending Order objects.
     * This template will be used by services to publish new orders.
     * @return KafkaTemplate for Order messages.
     */
    @Bean
    public KafkaTemplate<String, Order> orderKafkaTemplate() {
        return new KafkaTemplate<>(orderProducerFactory());
    }

    // --- Kafka Producer Configuration for Trades ---

    /**
     * Configures the ProducerFactory for sending Trade objects to Kafka.
     * Uses StringSerializer for keys and JsonSerializer for Trade values.
     * @return ProducerFactory for Trade messages.
     */
    @Bean
    public ProducerFactory<String, Trade> tradeProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Creates a KafkaTemplate for sending Trade objects.
     * This template will be used by the matching engine to publish executed trades.
     * @return KafkaTemplate for Trade messages.
     */
    @Bean
    public KafkaTemplate<String, Trade> tradeKafkaTemplate() {
        return new KafkaTemplate<>(tradeProducerFactory());
    }

    // NEW: --- Kafka Producer Configuration for Market Data Updates (OrderBookSummary) ---

    /**
     * Configures the ProducerFactory for sending OrderBookSummary objects to Kafka.
     * Uses StringSerializer for keys and JsonSerializer for OrderBookSummary values.
     * @return ProducerFactory for OrderBookSummary messages.
     */
    @Bean
    public ProducerFactory<String, OrderBookSummary> marketDataProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Creates a KafkaTemplate for sending OrderBookSummary objects.
     * This template will be used by the MarketDataService to publish market data snapshots.
     * @return KafkaTemplate for OrderBookSummary messages.
     */
    @Bean
    public KafkaTemplate<String, OrderBookSummary> marketDataKafkaTemplate() {
        return new KafkaTemplate<>(marketDataProducerFactory());
    }

    // --- Kafka Consumer Configuration for Orders ---

    /**
     * Configures the ConsumerFactory for receiving Order objects from Kafka.
     * Uses StringDeserializer for keys and JsonDeserializer for Order values.
     * It also configures trusted packages and sets the default type for deserialization.
     * @return ConsumerFactory for Order messages.
     */
    @Bean
    public ConsumerFactory<String, Order> orderConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        // Trust all packages for deserialization; in production, specify exact packages
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*"); 
        // Specify default type for deserialization to correctly map JSON to Order object
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Order.class); 
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Creates a ConcurrentKafkaListenerContainerFactory for processing Order messages.
     * This factory is used by @KafkaListener annotations to create consumer containers.
     * @return ConcurrentKafkaListenerContainerFactory for Order messages.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Order> orderKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Order> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderConsumerFactory());
        return factory;
    }


    // --- Kafka Topic Creation ---

    /**
     * Defines a new Kafka topic for order submissions.
     * Spring Boot will automatically create this topic if it doesn't exist on startup.
     * @return NewTopic object for order submission topic.
     */
    @Bean
    public NewTopic orderSubmissionTopic() {
        return TopicBuilder.name(orderSubmissionTopic)
                .partitions(1) // Number of partitions for the topic
                .replicas(1)   // Number of replicas for the topic (should be >= 1)
                .build();
    }

    /**
     * Defines a new Kafka topic for trade executions.
     * Spring Boot will automatically create this topic if it doesn't exist on startup.
     * @return NewTopic object for trade execution topic.
     */
    @Bean
    public NewTopic tradeExecutionTopic() {
        return TopicBuilder.name(tradeExecutionTopic)
                .partitions(1) // Number of partitions for the topic
                .replicas(1)   // Number of replicas for the topic
                .build();
    }

    // NEW: Defines a new Kafka topic for market data updates.
    /**
     * Defines a new Kafka topic for market data updates.
     * Spring Boot will automatically create this topic if it doesn't exist on startup.
     * @return NewTopic object for market data updates topic.
     */
    @Bean
    public NewTopic marketDataUpdatesTopic() {
        return TopicBuilder.name(marketDataUpdatesTopic)
                .partitions(1) // Adjust partitions and replicas as needed for market data
                .replicas(1)
                .build();
    }

}