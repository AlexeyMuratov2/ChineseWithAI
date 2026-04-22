package ru.chinesewithai.backend.grammarexercise.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GrammarExerciseGenerationProperties.class)
public class GrammarExerciseConfiguration {}
