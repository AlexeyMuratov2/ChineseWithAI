package ru.chinesewithai.backend.user.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataUserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {
    boolean existsByUsername(String username);

    Optional<UserJpaEntity> findByUsername(String username);
}
