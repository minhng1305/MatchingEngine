package com.project.matchingengine.config;

import org.springframework.context.annotation.Configuration;


@Configuration
public class KafkaConfig {
    
    // Kafka configuration properties can be defined here
    // For example, bootstrap servers, key and value serializers, etc.
    
    // @Bean
    // public ProducerFactory<String, Order> producerFactory() {
    //     Map<String, Object> configProps = new HashMap<>();
    //     configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    //     configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    //     configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    //     return new DefaultKafkaProducerFactory<>(configProps);
    // }
    
    // @Bean
    // public KafkaTemplate<String, Order> kafkaTemplate() {
    //     return new KafkaTemplate<>(producerFactory());
    // }
}