package ru.chinesewithai.backend.storedfile.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Public identifier for a stored blob. Other application modules should only ever reference this
 * value — never bucket names or object keys.
 */
public record StoredFileId(UUID value) {

    public StoredFileId {
        Objects.requireNonNull(value, "value");
    }

    public static StoredFileId of(UUID value) {
        return new StoredFileId(value);
    }

    public static StoredFileId random() {
        return new StoredFileId(UUID.randomUUID());
    }
}
