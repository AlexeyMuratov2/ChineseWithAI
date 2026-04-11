package ru.chinesewithai.backend.storedfile.application.command;

import java.util.Objects;
import java.util.UUID;

public record DeleteStoredFileCommand(UUID fileId) {

    public DeleteStoredFileCommand {
        Objects.requireNonNull(fileId, "fileId");
    }
}
