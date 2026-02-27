package com.project.matchingengine.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;


@Configuration
@Profile("!ingress")
public class KafkaConsumerConfig {

    @Value("${spring.kafka.consumer.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroupId;

    @Value("${app.kafka.consumer.concurrency:4}")
    private int concurrency;

    @Value("${app.kafka.dlq.topic:orders-dlq}")
    private String dlqTopic;

    @Value("${spring.kafka.consumer.properties.security.protocol:#{null}}")
    private String securityProtocol;

    @Value("${spring.kafka.consumer.properties.sasl.mechanism:#{null}}")
    private String saslMechanism;

    @Value("${spring.kafka.consumer.properties.sasl.jaas.config:#{null}}")
    private String saslJaasConfig;

    @Autowired(required = false)
    private KafkaTemplate<String, String> kafkaTemplate;

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        if (securityProtocol != null && !securityProtocol.isEmpty()) {
            configProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);
            configProps.put(SaslConfigs.SASL_MECHANISM, saslMechanism);
            configProps.put(SaslConfigs.SASL_JAAS_CONFIG, saslJaasConfig);
        }

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * Error handler with Dead Letter Queue support.
     * Retries failed messages 3 times with 1 second delay.
     * After retries exhausted, sends to DLQ topic.
     */
    @Bean
    public DefaultErrorHandler errorHandler() {
        if (kafkaTemplate != null) {
            // DeadLetterPublishingRecoverer: routes failed messages to DLQ topic
            // The BiFunction receives (ConsumerRecord, Exception) and returns TopicPartition
            DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> {
                    // Route failed messages to DLQ topic
                    // Create TopicPartition with DLQ topic name and partition 0
                    // (DLQ topic should have partitions, but we use partition 0 as default)
                    return new TopicPartition(dlqTopic, 0);
                }
            );
            // Retry 3 times with 1 second delay between retries
            FixedBackOff backOff = new FixedBackOff(1000L, 3L);
            return new DefaultErrorHandler(recoverer, backOff);
        }
        // Fallback: simple retry without DLQ if KafkaTemplate not available
        FixedBackOff backOff = new FixedBackOff(1000L, 3L);
        return new DefaultErrorHandler(backOff);
    }

    /**
     * Kafka listener container factory with batch processing and error handling.
     * Concurrency is configurable via app.kafka.consumer.concurrency property.
     * Formula: concurrency = total_partitions / number_of_servers
     * Example: 12 partitions / 3 servers = 4 threads per server
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(concurrency);
        factory.setBatchListener(true);
        factory.setCommonErrorHandler(errorHandler());
        // Optimize poll settings
        Map<String, Object> props = new HashMap<>(factory.getConsumerFactory().getConfigurationProperties());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500); // Process up to 500 records per poll
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024 * 1024); // 1MB minimum fetch
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500); // Max wait 500ms
        factory.getContainerProperties().setPollTimeout(1500);
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(props));
        return factory;
    }
}