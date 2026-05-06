package ru.chinesewithai.backend.lesson.application.source;

import java.util.List;
import java.util.UUID;

public record LessonSourcePackItem(
        UUID sourceId,
        int position,
        String mediaCategory,
        String originalFileName,
        String normalizedText,
        List<String> warnings) {

    public LessonSourcePackItem {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
