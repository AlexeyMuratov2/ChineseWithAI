package ru.chinesewithai.backend.lessondraft.application.view;

import java.time.Instant;
import java.util.UUID;

public record LessonDraftSourceView(
        UUID id,
        String type,
        int position,
        String textContent,
        UUID documentFileId,
        String documentOriginalFileName,
        Instant createdAt,
        Instant updatedAt) {}
