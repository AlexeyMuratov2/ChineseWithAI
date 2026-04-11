package ru.chinesewithai.backend.lessondraft.application.exception;

import java.util.UUID;

public class LessonDraftNotFoundException extends RuntimeException {

    public LessonDraftNotFoundException(UUID draftId) {
        super("Lesson draft not found: " + draftId);
    }
}
