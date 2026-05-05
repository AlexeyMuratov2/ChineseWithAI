package ru.chinesewithai.backend.lesson.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lesson_generation_run_stages")
public class LessonGenerationRunStageJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "stage_index", nullable = false)
    private int stageIndex;

    @Column(name = "stage_key", nullable = false, length = 120)
    private String stageKey;

    @Column(name = "agent_session_id")
    private UUID agentSessionId;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "output_json")
    private String outputJson;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "finished_at", nullable = false)
    private Instant finishedAt;

    protected LessonGenerationRunStageJpaEntity() {}

    LessonGenerationRunStageJpaEntity(
            UUID id,
            UUID runId,
            int stageIndex,
            String stageKey,
            UUID agentSessionId,
            String status,
            String outputJson,
            String failureReason,
            Instant createdAt,
            Instant finishedAt) {
        this.id = id;
        this.runId = runId;
        this.stageIndex = stageIndex;
        this.stageKey = stageKey;
        this.agentSessionId = agentSessionId;
        this.status = status;
        this.outputJson = outputJson;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.finishedAt = finishedAt;
    }
}
