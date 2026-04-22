package ru.chinesewithai.backend.learnerprofile.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataLearnerProfileContextJpaRepository extends JpaRepository<LearnerProfileContextJpaEntity, String> {

    Optional<LearnerProfileContextJpaEntity> findByProfileKeyAndActiveTrue(String profileKey);
}
