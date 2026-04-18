package ru.chinesewithai.backend.lesson.application.validation;

import com.fasterxml.jackson.databind.JsonNode;
import ru.chinesewithai.backend.lesson.domain.model.LessonModule;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftView;

public interface LessonModuleStrategy {
    String moduleKey();

    void validateDraftForGeneration(LessonDraftView draft);

    void validateLesson(JsonNode lessonJson, LessonModule module);

    String generationInstructions();
}
