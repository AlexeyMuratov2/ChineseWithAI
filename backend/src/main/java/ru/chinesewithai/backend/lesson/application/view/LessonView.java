package ru.chinesewithai.backend.lesson.application.view;

import java.time.Instant;
import java.util.UUID;

public record LessonView(
        UUID id,
        UUID ownerId,
        String moduleKey,
        UUID sourceDraftId,
        UUID generatorSessionId,
        String title,
        String studyLanguage,
        String explanationLanguage,
        String translationLanguage,
        String contentJson,
        Instant createdAt,
        Instant updatedAt,
        long version) {}
