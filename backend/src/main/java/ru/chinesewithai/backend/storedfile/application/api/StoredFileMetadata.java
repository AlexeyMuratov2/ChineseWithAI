package ru.chinesewithai.backend.storedfile.application.api;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import ru.chinesewithai.backend.storedfile.domain.model.StoredFile;

/** Technical metadata exposed to other modules (no storage keys, no bucket). */
public record StoredFileMetadata(
        UUID id, long sizeBytes, Optional<String> contentType, Optional<String> originalFileName, Instant createdAt) {

    public static StoredFileMetadata from(StoredFile file) {
        return new StoredFileMetadata(
                file.id().value(),
                file.sizeBytes(),
                file.contentType(),
                file.originalFileName(),
                file.createdAt());
    }
}
