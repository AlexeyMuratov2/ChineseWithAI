package ru.chinesewithai.backend.storedfile.infrastructure.persistence;

import ru.chinesewithai.backend.storedfile.domain.model.StoredFile;
import ru.chinesewithai.backend.storedfile.domain.model.StoredFileId;

public final class StoredFileJpaMapper {

    private StoredFileJpaMapper() {}

    public static StoredFile toDomain(StoredFileJpaEntity entity) {
        return new StoredFile(
                StoredFileId.of(entity.getId()),
                entity.getSizeBytes(),
                entity.getContentType(),
                entity.getOriginalFileName(),
                entity.getCreatedAt());
    }
}
