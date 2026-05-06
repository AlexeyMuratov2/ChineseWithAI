package ru.chinesewithai.backend.lesson.application.source;

import java.util.List;
import java.util.Objects;

public record LessonSourceBundle(int sourceBundleVersion, List<LessonSourceManifestItem> sources) {

    public LessonSourceBundle {
        if (sourceBundleVersion <= 0) {
            throw new IllegalArgumentException("sourceBundleVersion must be > 0");
        }
        Objects.requireNonNull(sources, "sources must not be null");
        sources = List.copyOf(sources);
    }
}
