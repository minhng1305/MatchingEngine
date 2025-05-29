// package com.project.matchingengine.config;

// import org.springframework.cache.annotation.EnableCaching;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.data.redis.cache.RedisCacheManager;
// import org.springframework.data.redis.connection.RedisConnectionFactory;
// import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
// import org.springframework.data.redis.core.RedisTemplate;
// import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
// import org.springframework.data.redis.serializer.StringRedisSerializer;
// import com.fasterxml.jackson.databind.DeserializationFeature;
// import com.fasterxml.jackson.databind.SerializationFeature;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;



// @Configuration
// @EnableCaching
// public class RedisConfig {

//     @Bean
//     public RedisConnectionFactory redisConnectionFactory() 
//     {
//         LettuceConnectionFactory factory = new LettuceConnectionFactory();
//         return factory;
//     }


//     @Bean
//     public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) 
//     {
//         return RedisCacheManager.create(connectionFactory);
//     }


//     @Bean
//     public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) 
//     {
//         RedisTemplate<String, Object> template = new RedisTemplate<>();
//         template.setConnectionFactory(connectionFactory);

//         // Configure Jackson for better error handling
//         ObjectMapper objectMapper = new ObjectMapper()
//             .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
//             .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

//         // Create serializer with configured ObjectMapper
//         Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
//         serializer.setObjectMapper(objectMapper);

//         template.setDefaultSerializer(serializer);
        
//         // Use StringRedisSerializer for keys
//         template.setKeySerializer(new StringRedisSerializer());
        
//         // Use JSON serializer for values
//         template.setValueSerializer(serializer);
        
//         // Also for hash keys and values
//         template.setHashKeySerializer(new StringRedisSerializer());
//         template.setHashValueSerializer(serializer);

        
//         template.afterPropertiesSet();
//         return template;
//     }
// }