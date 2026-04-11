package ru.chinesewithai.backend.lessondraft.api.dto;

import java.time.Instant;
import java.util.UUID;

public record LessonDraftSummaryResponse(
        UUID id,
        String title,
        String explanationLanguage,
        String translationLanguage,
        int sourceCount,
        Instant createdAt,
        Instant updatedAt,
        long version) {}
