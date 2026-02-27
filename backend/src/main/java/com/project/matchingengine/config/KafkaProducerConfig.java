package com.project.matchingengine.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;


@Configuration
public class KafkaProducerConfig {
    @Value("${spring.kafka.producer.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.producer.properties.security.protocol:#{null}}")
    private String securityProtocol;

    @Value("${spring.kafka.producer.properties.sasl.mechanism:#{null}}")
    private String saslMechanism;

    @Value("${spring.kafka.producer.properties.sasl.jaas.config:#{null}}")
    private String saslJaasConfig;

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        
        // Idempotent producer: ensures exactly-once semantics
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        // Acks=all: wait for all replicas to acknowledge (highest durability)
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        
        // Retry on transient failures
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        
        // For idempotence: max in-flight requests per connection must be <= 5
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        // Performance tuning (Ingress → Kafka: batching + compression)
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 65536);   // 64 KB
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);       // 10 ms
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 67_108_864); // 64 MB (default 32 MB)

        if (securityProtocol != null && !securityProtocol.isEmpty()) {
            configProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);
            configProps.put(SaslConfigs.SASL_MECHANISM, saslMechanism);
            configProps.put(SaslConfigs.SASL_JAAS_CONFIG, saslJaasConfig);
        }

        return new DefaultKafkaProducerFactory<>(configProps);
    }


    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}