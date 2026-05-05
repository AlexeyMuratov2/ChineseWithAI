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
    @Query("select d from LessonDraftJpaEntity d where d.id = :id")
    Optional<LessonDraftJpaEntity> findWithSourcesById(UUID id);

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
            group by d.id, d.title, d.explanationLanguage, d.translationLanguage, d.createdAt, d.updatedAt, d.version
            order by d.updatedAt desc
            """)
    Page<LessonDraftListItemJpaProjection> findPage(Pageable pageable);
}
