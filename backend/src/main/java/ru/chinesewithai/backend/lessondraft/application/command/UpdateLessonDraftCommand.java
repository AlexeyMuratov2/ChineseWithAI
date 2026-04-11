package ru.chinesewithai.backend.lessondraft.application.command;

import java.util.Objects;
import java.util.UUID;

public record UpdateLessonDraftCommand(
        UUID draftId, String title, String description, String userInstructions, String explanationLanguage, String translationLanguage) {

    public UpdateLessonDraftCommand {
        Objects.requireNonNull(draftId, "draftId must not be null");
        Objects.requireNonNull(title, "title must not be null");
    }
}
