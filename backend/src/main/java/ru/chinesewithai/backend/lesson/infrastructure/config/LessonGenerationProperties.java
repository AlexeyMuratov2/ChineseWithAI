package ru.chinesewithai.backend.lesson.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.lesson.generation")
public record LessonGenerationProperties(String defaultModelKey) {

    public LessonGenerationProperties {
        if (defaultModelKey == null || defaultModelKey.isBlank()) {
            defaultModelKey = "deepseek-chat";
        }
    }
}
