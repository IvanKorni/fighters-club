package net.proselyte.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("application.telegram")
public record TelegramProperties(
        String botToken
) {
}




