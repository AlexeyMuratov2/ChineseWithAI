package ru.chinesewithai.backend.lessondraft.application.view;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LessonDraftView(
        UUID id,
        String title,
        String description,
        String userInstructions,
        String explanationLanguage,
        String translationLanguage,
        List<LessonDraftSourceView> sources,
        Instant createdAt,
        Instant updatedAt,
        long version) {}
