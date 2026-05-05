package ru.chinesewithai.backend.lesson.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lesson_generation_runs")
public class LessonGenerationRunJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "draft_id", nullable = false)
    private UUID draftId;

    @Column(name = "module_key", nullable = false, length = 120)
    private String moduleKey;

    @Column(name = "pipeline_key", nullable = false, length = 120)
    private String pipelineKey;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "lesson_id")
    private UUID lessonId;

    @Column(name = "final_generator_session_id")
    private UUID finalGeneratorSessionId;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LessonGenerationRunJpaEntity() {}

    LessonGenerationRunJpaEntity(
            UUID id,
            UUID draftId,
            String moduleKey,
            String pipelineKey,
            String status,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.draftId = draftId;
        this.moduleKey = moduleKey;
        this.pipelineKey = pipelineKey;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    void markCompleted(UUID lessonId, UUID finalGeneratorSessionId, Instant finishedAt) {
        this.status = "COMPLETED";
        this.lessonId = lessonId;
        this.finalGeneratorSessionId = finalGeneratorSessionId;
        this.failureReason = null;
        this.finishedAt = finishedAt;
        this.updatedAt = finishedAt;
    }

    void markFailed(String failureReason, Instant finishedAt) {
        this.status = "FAILED";
        this.failureReason = failureReason;
        this.finishedAt = finishedAt;
        this.updatedAt = finishedAt;
    }
}
