package ru.chinesewithai.backend.lessondraft.application.exception;

import java.util.UUID;

public class SourceNotFoundException extends RuntimeException {

    public SourceNotFoundException(UUID sourceId) {
        super("Lesson draft source not found: " + sourceId);
    }
}
