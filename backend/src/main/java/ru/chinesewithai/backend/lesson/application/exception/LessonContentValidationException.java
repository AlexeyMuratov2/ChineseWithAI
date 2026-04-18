package ru.chinesewithai.backend.lesson.application.exception;

public class LessonContentValidationException extends RuntimeException {

    public LessonContentValidationException(String message) {
        super(message);
    }
}
