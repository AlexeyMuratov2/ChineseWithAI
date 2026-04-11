package ru.chinesewithai.backend.lessondraft.application.view;

import java.time.Instant;
import java.util.UUID;

public record LessonDraftSummaryView(
        UUID id,
        String title,
        String explanationLanguage,
        String translationLanguage,
        int sourceCount,
        Instant createdAt,
        Instant updatedAt,
        long version) {}
