package ru.chinesewithai.backend.lessondraft.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LessonDraftResponse(
        UUID id,
        UUID ownerId,
        String title,
        String description,
        String userInstructions,
        String explanationLanguage,
        String translationLanguage,
        List<LessonDraftSourceResponse> sources,
        Instant createdAt,
        Instant updatedAt,
        long version) {}
