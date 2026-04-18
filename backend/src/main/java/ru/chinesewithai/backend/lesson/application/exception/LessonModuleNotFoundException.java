package ru.chinesewithai.backend.lesson.application.exception;

public class LessonModuleNotFoundException extends RuntimeException {

    public LessonModuleNotFoundException(String moduleKey) {
        super("Lesson module not found: " + moduleKey);
    }
}
