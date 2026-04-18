package ru.chinesewithai.backend.lesson.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record LessonResponse(
        UUID id,
        UUID ownerId,
        String moduleKey,
        UUID sourceDraftId,
        UUID generatorSessionId,
        String title,
        String studyLanguage,
        String explanationLanguage,
        String translationLanguage,
        JsonNode content,
        Instant createdAt,
        Instant updatedAt,
        long version) {}
