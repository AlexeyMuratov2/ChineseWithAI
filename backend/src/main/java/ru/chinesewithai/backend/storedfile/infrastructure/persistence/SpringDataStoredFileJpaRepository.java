package ru.chinesewithai.backend.storedfile.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataStoredFileJpaRepository extends JpaRepository<StoredFileJpaEntity, UUID> {}
