// package com.project.matchingengine.service;

// import org.springframework.data.redis.serializer.RedisSerializer;
// import org.springframework.data.redis.serializer.SerializationException;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


// public class CustomRedisSerializer implements RedisSerializer<Object> {
//     private final ObjectMapper objectMapper;

//     public CustomRedisSerializer() {
//         this.objectMapper = new ObjectMapper();
//         objectMapper.registerModule(new JavaTimeModule()); 
//         // Configure any other ObjectMapper settings
//     }

//     @Override
//     public byte[] serialize(Object t) throws SerializationException {
//         if (t == null) {
//             return new byte[0];
//         }
//         try {
//             return objectMapper.writeValueAsBytes(t);
//         } catch (Exception ex) {
//             throw new SerializationException("Could not serialize object", ex);
//         }
//     }

//     @Override
//     public Object deserialize(byte[] bytes) throws SerializationException {
//         if (bytes == null || bytes.length == 0) {
//             return null;
//         }
//         try {
//             return objectMapper.readValue(bytes, Object.class);
//         } catch (Exception ex) {
//             throw new SerializationException("Could not deserialize object", ex);
//         }
//     }
// }