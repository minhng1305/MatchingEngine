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
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Registers the "/gs-guide-websocket" endpoint, enabling STOMP over WebSocket. [2]
        // This is the path clients will use to connect to the WebSocket server. [1]
        // withSockJS() provides fallback options for browsers that don't support WebSocket.
        registry.addEndpoint("/ws").withSockJS();
    }
    
    // Example:
    // @Configuration
    // @EnableWebSocket
    // public class WebSocketConfig implements WebSocketConfigurer {
    //     @Override
    //     public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    //         registry.addHandler(new YourWebSocketHandler(), "/ws-endpoint");
    //     }
    // }
}