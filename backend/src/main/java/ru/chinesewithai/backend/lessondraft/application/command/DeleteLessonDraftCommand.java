package ru.chinesewithai.backend.lessondraft.application.command;

import java.util.Objects;
import java.util.UUID;

public record DeleteLessonDraftCommand(UUID draftId) {

    public DeleteLessonDraftCommand {
        Objects.requireNonNull(draftId, "draftId must not be null");
    }
}
