package ru.chinesewithai.backend.lessondraft.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SpringDataLessonDraftJpaRepository extends JpaRepository<LessonDraftJpaEntity, UUID> {

    @EntityGraph(attributePaths = "sources")
    Optional<LessonDraftJpaEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    @Query(
            """
            select new ru.chinesewithai.backend.lessondraft.infrastructure.persistence.LessonDraftListItemJpaProjection(
                d.id,
                d.title,
                d.explanationLanguage,
                d.translationLanguage,
                count(s.id),
                d.createdAt,
                d.updatedAt,
                d.version
            )
            from LessonDraftJpaEntity d
            left join d.sources s
            where d.ownerId = :ownerId
            group by d.id, d.title, d.explanationLanguage, d.translationLanguage, d.createdAt, d.updatedAt, d.version
            order by d.updatedAt desc
            """)
    Page<LessonDraftListItemJpaProjection> findPageByOwnerId(UUID ownerId, Pageable pageable);
}
