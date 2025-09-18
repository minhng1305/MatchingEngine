package com.project.matchingengine.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;



@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer  {
    // private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple memory-based message broker to carry messages
        config.enableSimpleBroker("/topic", "/queue");
        // Prefix for messages bound for @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
        // Prefix for user-specific destinations
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP endpoint
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    // @Override
    // public void configureMessageBroker(MessageBrokerRegistry config) {
    //     // Enable simple broker for destinations prefixed with "/topic" and "/queue"
    //     config.enableSimpleBroker("/topic", "/queue")
    //           .setHeartbeatValue(new long[]{10000, 20000})
    //           .setTaskScheduler(null); // Use default task scheduler
        
    //     // Set prefix for messages bound for @MessageMapping methods
    //     config.setApplicationDestinationPrefixes("/app");
        
    //     // Set prefix for user-specific destinations
    //     config.setUserDestinationPrefix("/user");
    // }

    // @Override
    // public void registerStompEndpoints(StompEndpointRegistry registry) {
    //     // Register STOMP endpoint with SockJS support
    //     registry.addEndpoint("/ws")
    //             .setAllowedOriginPatterns("*")
    //             .withSockJS()
    //             .setWebSocketEnabled(true)
    //             .setHttpMessageCacheSize(1000)
    //             .setDisconnectDelay(30 * 1000);
    // }
}