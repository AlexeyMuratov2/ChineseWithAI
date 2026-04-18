package ru.chinesewithai.backend.lesson.application.command;

import java.util.Objects;
import java.util.UUID;

public record GenerateLessonFromDraftCommand(UUID draftId, String moduleKey, String modelKey) {

    public GenerateLessonFromDraftCommand {
        Objects.requireNonNull(draftId, "draftId must not be null");
        Objects.requireNonNull(moduleKey, "moduleKey must not be null");
    }
}
