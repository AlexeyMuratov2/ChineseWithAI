package ru.chinesewithai.backend.lesson.application.source;

import java.util.Objects;
import java.util.Set;

public record LessonSourceProcessingPolicy(
        LessonSourceProcessingMode mode,
        int minSources,
        int maxSources,
        Set<String> allowedSourceTypes,
        Set<String> allowedMediaCategories,
        PdfHandlingMode pdfHandlingMode,
        boolean attachImagesToVisionStages,
        int maxInlineImageBytes,
        int maxPdfRenderedPages) {

    public LessonSourceProcessingPolicy {
        Objects.requireNonNull(mode, "mode must not be null");
        Objects.requireNonNull(allowedSourceTypes, "allowedSourceTypes must not be null");
        Objects.requireNonNull(allowedMediaCategories, "allowedMediaCategories must not be null");
        Objects.requireNonNull(pdfHandlingMode, "pdfHandlingMode must not be null");
        if (minSources < 1) {
            throw new IllegalArgumentException("minSources must be >= 1");
        }
        if (maxSources < minSources) {
            throw new IllegalArgumentException("maxSources must be >= minSources");
        }
        if (allowedSourceTypes.isEmpty()) {
            throw new IllegalArgumentException("allowedSourceTypes must not be empty");
        }
        if (allowedMediaCategories.isEmpty()) {
            throw new IllegalArgumentException("allowedMediaCategories must not be empty");
        }
        if (maxInlineImageBytes < 1) {
            throw new IllegalArgumentException("maxInlineImageBytes must be >= 1");
        }
        if (maxPdfRenderedPages < 1) {
            throw new IllegalArgumentException("maxPdfRenderedPages must be >= 1");
        }
        allowedSourceTypes = Set.copyOf(allowedSourceTypes);
        allowedMediaCategories = Set.copyOf(allowedMediaCategories);
    }
}
