package ru.chinesewithai.backend.lesson.application.generation;

import java.util.Objects;
import ru.chinesewithai.backend.lesson.domain.model.LessonModule;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftView;

public record LessonGenerationPipelineRequest(LessonModule module, LessonDraftView draft, String modelKey) {

    public LessonGenerationPipelineRequest {
        Objects.requireNonNull(module, "module must not be null");
        Objects.requireNonNull(draft, "draft must not be null");
        Objects.requireNonNull(modelKey, "modelKey must not be null");
    }
}
