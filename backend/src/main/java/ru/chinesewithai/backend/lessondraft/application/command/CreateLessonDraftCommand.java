package ru.chinesewithai.backend.lessondraft.application.command;

import java.util.Objects;

public record CreateLessonDraftCommand(
        String title,
        String description,
        String userInstructions,
        String explanationLanguage,
        String translationLanguage) {

    public CreateLessonDraftCommand {
        Objects.requireNonNull(title, "title must not be null");
    }
}
