package ru.chinesewithai.backend.lessondraft.application.command;

import java.util.Objects;
import java.util.UUID;
import ru.chinesewithai.backend.lessondraft.domain.model.LessonDraftSourceType;

public record AddLessonDraftSourceCommand(
        UUID draftId,
        LessonDraftSourceType type,
        String textContent,
        UUID documentFileId,
        String documentOriginalFileName) {

    public AddLessonDraftSourceCommand {
        Objects.requireNonNull(draftId, "draftId must not be null");
        Objects.requireNonNull(type, "type must not be null");
    }
}
