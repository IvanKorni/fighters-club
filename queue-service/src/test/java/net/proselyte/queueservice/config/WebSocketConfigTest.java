package net.proselyte.queueservice.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = WebSocketConfig.class)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
@DisplayName("WebSocketConfig Integration Tests")
class WebSocketConfigTest {

    @Autowired(required = false)
    private WebSocketConfig webSocketConfig;

    @Test
    @DisplayName("Should load WebSocketConfig bean")
    void shouldLoadWebSocketConfigBean() {
        assertNotNull(webSocketConfig, "WebSocketConfig should be loaded");
    }

    @Test
    @DisplayName("Should have configureMessageBroker method")
    void shouldHaveConfigureMessageBrokerMethod() throws Exception {
        assertNotNull(webSocketConfig);
        
        Method method = WebSocketConfig.class.getMethod(
                "configureMessageBroker", 
                MessageBrokerRegistry.class
        );
        
        assertNotNull(method, "configureMessageBroker method should exist");
        assertTrue(WebSocketMessageBrokerConfigurer.class.isAssignableFrom(WebSocketConfig.class),
                "WebSocketConfig should implement WebSocketMessageBrokerConfigurer");
    }

    @Test
    @DisplayName("Should have registerStompEndpoints method")
    void shouldHaveRegisterStompEndpointsMethod() throws Exception {
        assertNotNull(webSocketConfig);
        
        Method method = WebSocketConfig.class.getMethod(
                "registerStompEndpoints", 
                StompEndpointRegistry.class
        );
        
        assertNotNull(method, "registerStompEndpoints method should exist");
    }

    @Test
    @DisplayName("Should be annotated with @Configuration")
    void shouldBeAnnotatedWithConfiguration() {
        assertNotNull(webSocketConfig);
        
        assertTrue(WebSocketConfig.class.isAnnotationPresent(
                org.springframework.context.annotation.Configuration.class),
                "WebSocketConfig should be annotated with @Configuration");
    }

    @Test
    @DisplayName("Should be annotated with @EnableWebSocketMessageBroker")
    void shouldBeAnnotatedWithEnableWebSocketMessageBroker() {
        assertNotNull(webSocketConfig);
        
        assertTrue(WebSocketConfig.class.isAnnotationPresent(
                org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker.class),
                "WebSocketConfig should be annotated with @EnableWebSocketMessageBroker");
    }
}

