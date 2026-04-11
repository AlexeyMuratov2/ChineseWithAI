package ru.chinesewithai.backend.lessondraft.application.command;

import java.util.Objects;
import java.util.UUID;

public record GetLessonDraftQuery(UUID draftId) {

    public GetLessonDraftQuery {
        Objects.requireNonNull(draftId, "draftId must not be null");
    }
}
