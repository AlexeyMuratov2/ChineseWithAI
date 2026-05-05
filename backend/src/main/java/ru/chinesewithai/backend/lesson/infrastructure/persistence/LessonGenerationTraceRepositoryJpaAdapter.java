package ru.chinesewithai.backend.lesson.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.chinesewithai.backend.lesson.application.port.out.LessonGenerationTraceRepository;

@Repository
public class LessonGenerationTraceRepositoryJpaAdapter implements LessonGenerationTraceRepository {

    private final SpringDataLessonGenerationRunJpaRepository runRepository;
    private final SpringDataLessonGenerationRunStageJpaRepository stageRepository;

    public LessonGenerationTraceRepositoryJpaAdapter(
            SpringDataLessonGenerationRunJpaRepository runRepository,
            SpringDataLessonGenerationRunStageJpaRepository stageRepository) {
        this.runRepository = runRepository;
        this.stageRepository = stageRepository;
    }

    @Override
    @Transactional
    public UUID startRun(UUID draftId, String moduleKey, String pipelineKey, Instant now) {
        var run = new LessonGenerationRunJpaEntity(
                UUID.randomUUID(), draftId, moduleKey, pipelineKey, "RUNNING", now, now);
        return runRepository.save(run).getId();
    }

    @Override
    @Transactional
    public void recordStageCompleted(
            UUID runId, int stageIndex, String stageKey, UUID agentSessionId, String outputJson, Instant finishedAt) {
        stageRepository.save(new LessonGenerationRunStageJpaEntity(
                UUID.randomUUID(),
                runId,
                stageIndex,
                stageKey,
                agentSessionId,
                "COMPLETED",
                outputJson,
                null,
                finishedAt,
                finishedAt));
    }

    @Override
    @Transactional
    public void recordStageFailed(
            UUID runId, int stageIndex, String stageKey, UUID agentSessionId, String failureReason, Instant finishedAt) {
        stageRepository.save(new LessonGenerationRunStageJpaEntity(
                UUID.randomUUID(),
                runId,
                stageIndex,
                stageKey,
                agentSessionId,
                "FAILED",
                null,
                failureReason,
                finishedAt,
                finishedAt));
    }

    @Override
    @Transactional
    public void markRunCompleted(UUID runId, UUID lessonId, UUID finalGeneratorSessionId, Instant finishedAt) {
        var run = runRepository
                .findById(runId)
                .orElseThrow(() -> new IllegalStateException("Missing lesson generation run: " + runId));
        run.markCompleted(lessonId, finalGeneratorSessionId, finishedAt);
        runRepository.save(run);
    }

    @Override
    @Transactional
    public void markRunFailed(UUID runId, String failureReason, Instant finishedAt) {
        var run = runRepository
                .findById(runId)
                .orElseThrow(() -> new IllegalStateException("Missing lesson generation run: " + runId));
        run.markFailed(failureReason, finishedAt);
        runRepository.save(run);
    }
}
