package ru.chinesewithai.backend.lessondraft.application.view;

import java.time.Instant;
import java.util.UUID;
import ru.chinesewithai.backend.lessondraft.domain.model.LessonDraftSourceType;

public record LessonDraftSourceView(
        UUID id,
        LessonDraftSourceType type,
        int position,
        String textContent,
        UUID documentFileId,
        String documentOriginalFileName,
        Instant createdAt,
        Instant updatedAt) {}
