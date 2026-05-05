package ru.chinesewithai.backend.lesson.application.port.out;

import java.time.Instant;
import java.util.UUID;

public interface LessonGenerationTraceRepository {

    UUID startRun(UUID draftId, String moduleKey, String pipelineKey, Instant now);

    void recordStageCompleted(
            UUID runId, int stageIndex, String stageKey, UUID agentSessionId, String outputJson, Instant finishedAt);

    void recordStageFailed(
            UUID runId, int stageIndex, String stageKey, UUID agentSessionId, String failureReason, Instant finishedAt);

    void markRunCompleted(UUID runId, UUID lessonId, UUID finalGeneratorSessionId, Instant finishedAt);

    void markRunFailed(UUID runId, String failureReason, Instant finishedAt);
}
