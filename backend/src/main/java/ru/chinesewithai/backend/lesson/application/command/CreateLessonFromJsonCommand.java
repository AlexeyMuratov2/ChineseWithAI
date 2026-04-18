package ru.chinesewithai.backend.lesson.application.command;

import java.util.Objects;
import java.util.UUID;

public record CreateLessonFromJsonCommand(String moduleKey, UUID sourceDraftId, String contentJson) {

    public CreateLessonFromJsonCommand {
        Objects.requireNonNull(contentJson, "contentJson must not be null");
    }
}
