package ru.chinesewithai.backend.lesson.application.exception;

import java.util.UUID;

public class LessonNotFoundException extends RuntimeException {

    public LessonNotFoundException(UUID lessonId) {
        super("Lesson not found: " + lessonId);
    }
}
