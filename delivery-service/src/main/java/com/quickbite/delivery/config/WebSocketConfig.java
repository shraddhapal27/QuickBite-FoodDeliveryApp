package com.quickbite.delivery.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Broadcasts to subscribers at /topic/delivery/{orderId}
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint configured for SockJS/STOMP.
        // It uses setAllowedOriginPatterns("*") because CORS is disabled in security config but websocket needs its own origin allowed.
        registry.addEndpoint("/ws-delivery")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
