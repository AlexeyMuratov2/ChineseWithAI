package ru.chinesewithai.backend.agentruntime.infrastructure.model;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.agentruntime.qwen")
public record QwenProperties(String baseUrl, String apiKey, boolean enableThinking) {

    public QwenProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        }
    }
}
