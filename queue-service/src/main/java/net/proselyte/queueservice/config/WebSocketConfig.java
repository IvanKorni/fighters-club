package net.proselyte.queueservice.config;

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
        // Включаем простой брокер сообщений для отправки сообщений клиентам
        config.enableSimpleBroker("/topic", "/queue");
        // Префикс для сообщений от клиента к серверу
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Регистрируем endpoint для WebSocket соединений
        registry.addEndpoint("/ws/queue")
                .setAllowedOriginPatterns("*") // В продакшене указать конкретные домены
                .withSockJS(); // Поддержка SockJS для fallback
    }
}
