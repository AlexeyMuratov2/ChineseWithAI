package ru.chinesewithai.backend.lessondraft.application.port.out;

import java.time.Instant;
import java.util.UUID;

public record LessonDraftListItem(
        UUID id,
        String title,
        String explanationLanguage,
        String translationLanguage,
        int sourceCount,
        Instant createdAt,
        Instant updatedAt,
        long version) {}
