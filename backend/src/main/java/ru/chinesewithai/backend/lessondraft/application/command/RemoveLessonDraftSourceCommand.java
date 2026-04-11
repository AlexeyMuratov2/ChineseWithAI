package ru.chinesewithai.backend.lessondraft.application.command;

import java.util.Objects;
import java.util.UUID;

public record RemoveLessonDraftSourceCommand(UUID draftId, UUID sourceId) {

    public RemoveLessonDraftSourceCommand {
        Objects.requireNonNull(draftId, "draftId must not be null");
        Objects.requireNonNull(sourceId, "sourceId must not be null");
    }
}
