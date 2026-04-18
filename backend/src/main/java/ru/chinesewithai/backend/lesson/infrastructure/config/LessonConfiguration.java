package ru.chinesewithai.backend.lesson.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LessonGenerationProperties.class)
public class LessonConfiguration {}
