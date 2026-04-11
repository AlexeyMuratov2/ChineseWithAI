package ru.chinesewithai.backend.lessondraft.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

public record LessonDraftListItemJpaProjection(
        UUID id,
        String title,
        String explanationLanguage,
        String translationLanguage,
        long sourceCount,
        Instant createdAt,
        Instant updatedAt,
        long version) {}
