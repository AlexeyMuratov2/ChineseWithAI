package ru.chinesewithai.backend.storedfile.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Correlates client upload HTTP request, optional SSE channel, and resulting {@link StoredFileId}. */
public record FileUploadSessionId(UUID value) {

    public FileUploadSessionId {
        Objects.requireNonNull(value, "value");
    }

    public static FileUploadSessionId random() {
        return new FileUploadSessionId(UUID.randomUUID());
    }
}
