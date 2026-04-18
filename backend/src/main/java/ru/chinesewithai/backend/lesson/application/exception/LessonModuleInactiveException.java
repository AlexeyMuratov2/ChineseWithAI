package ru.chinesewithai.backend.lesson.application.exception;

public class LessonModuleInactiveException extends RuntimeException {

    public LessonModuleInactiveException(String moduleKey) {
        super("Lesson module is inactive: " + moduleKey);
    }
}
