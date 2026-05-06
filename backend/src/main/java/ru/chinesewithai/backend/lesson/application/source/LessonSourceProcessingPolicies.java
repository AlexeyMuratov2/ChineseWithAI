package ru.chinesewithai.backend.lesson.application.source;

import java.util.Set;

public final class LessonSourceProcessingPolicies {

    private LessonSourceProcessingPolicies() {}

    public static LessonSourceProcessingPolicy hsk5V2NormalizeFirst() {
        return new LessonSourceProcessingPolicy(
                LessonSourceProcessingMode.NORMALIZE_FIRST,
                1,
                40,
                Set.of("TEXT_NOTE", "DOCUMENT_FILE"),
                Set.of("text", "image", "pdf"),
                PdfHandlingMode.RENDER_TO_IMAGES_WHEN_NO_TEXT,
                true,
                5 * 1024 * 1024,
                20);
    }
}
