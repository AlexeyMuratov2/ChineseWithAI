package ru.chinesewithai.backend.lessondraft.application.command;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ReorderLessonDraftSourcesCommand(UUID draftId, List<UUID> orderedSourceIds) {

    public ReorderLessonDraftSourcesCommand {
        Objects.requireNonNull(draftId, "draftId must not be null");
        Objects.requireNonNull(orderedSourceIds, "orderedSourceIds must not be null");
    }
}
