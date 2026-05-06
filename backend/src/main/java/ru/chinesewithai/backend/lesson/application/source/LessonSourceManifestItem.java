package ru.chinesewithai.backend.lesson.application.source;

import java.util.UUID;

public record LessonSourceManifestItem(
        UUID sourceId,
        String type,
        int position,
        String textContent,
        UUID fileId,
        String originalFileName,
        String contentType,
        Long sizeBytes,
        String mediaCategory) {}
