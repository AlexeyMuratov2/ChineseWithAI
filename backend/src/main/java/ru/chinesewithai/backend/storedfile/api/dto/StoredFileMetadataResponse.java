package ru.chinesewithai.backend.storedfile.api.dto;

import java.time.Instant;
import java.util.UUID;
import ru.chinesewithai.backend.storedfile.application.api.StoredFileMetadata;

public record StoredFileMetadataResponse(
        UUID id, long sizeBytes, String contentType, String originalFileName, Instant createdAt) {

    public static StoredFileMetadataResponse from(StoredFileMetadata m) {
        return new StoredFileMetadataResponse(
                m.id(),
                m.sizeBytes(),
                m.contentType().orElse(null),
                m.originalFileName().orElse(null),
                m.createdAt());
    }
}
