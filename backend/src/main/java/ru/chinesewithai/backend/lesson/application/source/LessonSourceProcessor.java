package ru.chinesewithai.backend.lesson.application.source;

public interface LessonSourceProcessor {

    LessonSourceProcessingMode mode();

    LessonSourceProcessingResult process(LessonSourceProcessingRequest request);
}
