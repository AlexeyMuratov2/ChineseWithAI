package ru.chinesewithai.backend.grammarexercise.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.grammar-exercise.generation")
public record GrammarExerciseGenerationProperties(String defaultModelKey) {

    public GrammarExerciseGenerationProperties {
        if (defaultModelKey == null || defaultModelKey.isBlank()) {
            defaultModelKey = "deepseek-chat";
        }
    }
}
