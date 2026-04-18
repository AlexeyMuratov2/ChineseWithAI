package ru.chinesewithai.backend.lessondraft.api.dto;

import java.time.Instant;
import java.util.UUID;

public record LessonDraftSourceResponse(
        UUID id,
        String type,
        int position,
        String textContent,
        UUID documentFileId,
        String documentOriginalFileName,
        Instant createdAt,
        Instant updatedAt) {}
