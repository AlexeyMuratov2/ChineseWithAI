package ru.chinesewithai.backend.lesson.application.source;

import java.util.List;
import java.util.Objects;

public record LessonSourcePack(
        int sourcePackVersion,
        List<LessonSourcePackItem> sources,
        String combinedText,
        List<LessonSourceRef> sourceRefs) {

    public LessonSourcePack {
        if (sourcePackVersion <= 0) {
            throw new IllegalArgumentException("sourcePackVersion must be > 0");
        }
        Objects.requireNonNull(sources, "sources must not be null");
        combinedText = combinedText == null ? "" : combinedText;
        sourceRefs = sourceRefs == null ? List.of() : List.copyOf(sourceRefs);
        sources = List.copyOf(sources);
    }
}
