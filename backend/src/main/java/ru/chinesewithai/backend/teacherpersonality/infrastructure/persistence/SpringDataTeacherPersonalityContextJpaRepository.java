package ru.chinesewithai.backend.teacherpersonality.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataTeacherPersonalityContextJpaRepository
        extends JpaRepository<TeacherPersonalityContextJpaEntity, String> {

    Optional<TeacherPersonalityContextJpaEntity> findByProfileKeyAndActiveTrue(String profileKey);
}
