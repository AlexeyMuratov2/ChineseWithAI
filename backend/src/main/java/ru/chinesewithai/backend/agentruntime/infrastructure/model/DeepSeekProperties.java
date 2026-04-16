package ru.chinesewithai.backend.agentruntime.infrastructure.model;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.agentruntime.deepseek")
public record DeepSeekProperties(String baseUrl, String apiKey) {

    public DeepSeekProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.deepseek.com";
        }
    }
}
