package ru.chinesewithai.backend.lesson.application.generation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.UUID;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.agentruntime.application.command.StartAgentSessionCommand;
import ru.chinesewithai.backend.agentruntime.application.port.in.StartAgentSessionUseCase;
import ru.chinesewithai.backend.agentruntime.application.view.AgentSessionView;
import ru.chinesewithai.backend.lesson.application.exception.LessonContentValidationException;
import ru.chinesewithai.backend.lesson.application.exception.LessonGenerationFailedException;
import ru.chinesewithai.backend.lesson.application.port.out.LessonGenerationTraceRepository;
import ru.chinesewithai.backend.lesson.application.source.LessonSourceProcessingPolicies;
import ru.chinesewithai.backend.lesson.application.source.LessonSourceProcessingPolicy;
import ru.chinesewithai.backend.lesson.application.source.LessonSourceProcessingRequest;
import ru.chinesewithai.backend.lesson.application.source.LessonSourceProcessorCatalog;
import ru.chinesewithai.backend.lesson.application.source.LessonSourcePackValidator;

@Component
public class Hsk5V2SourceNormalizedLessonGenerationPipeline implements LessonGenerationPipeline {

    public static final String PIPELINE_KEY = "hsk5-source-normalized:v2";

    private static final String SOURCE_NORMALIZER_PROFILE = "lesson-stage:hsk5_v2_source_normalizer";
    private static final String COMPOSER_PROFILE = "lesson-generator:hsk5_v2_composer";

    private final StartAgentSessionUseCase startAgentSessionUseCase;
    private final LessonGenerationTraceRepository traceRepository;
    private final LessonSourceProcessorCatalog sourceProcessorCatalog;
    private final LessonSourcePackValidator sourcePackValidator;
    private final ObjectMapper objectMapper;

    public Hsk5V2SourceNormalizedLessonGenerationPipeline(
            StartAgentSessionUseCase startAgentSessionUseCase,
            LessonGenerationTraceRepository traceRepository,
            LessonSourceProcessorCatalog sourceProcessorCatalog,
            LessonSourcePackValidator sourcePackValidator,
            ObjectMapper objectMapper) {
        this.startAgentSessionUseCase = startAgentSessionUseCase;
        this.traceRepository = traceRepository;
        this.sourceProcessorCatalog = sourceProcessorCatalog;
        this.sourcePackValidator = sourcePackValidator;
        this.objectMapper = objectMapper;
    }

    @Override
    public String key() {
        return PIPELINE_KEY;
    }

    @Override
    public LessonGenerationPipelineResult generate(LessonGenerationPipelineRequest request) {
        var runId = traceRepository.startRun(
                request.draft().id(), request.module().moduleKey(), key(), Instant.now());
        try {
            var policy = LessonSourceProcessingPolicies.hsk5V2NormalizeFirst();
            var sourceProcessing = sourceProcessorCatalog
                    .getRequired(policy.mode())
                    .process(new LessonSourceProcessingRequest(request.draft(), policy));

            var sourceNormalizerInput = baseInput(request);
            sourceNormalizerInput.put("sourceProcessingMode", sourceProcessing.mode().name());
            sourceNormalizerInput.put("sourceProcessingPolicy", sourceProcessingPolicyPayload(policy));
            sourceNormalizerInput.put("sourceBundle", sourceProcessing.sourceBundle());

            var sourcePack = runSourceNormalizationStage(
                    runId,
                    sourceNormalizerInput,
                    request.modelKey());

            var composerSession = runStageSession(
                    runId,
                    1,
                    "composer",
                    COMPOSER_PROFILE,
                    "Compose the hsk5_v2 source-normalized lesson JSON.",
                    composerInput(request, sourcePack),
                    composerPrompt(),
                    request.modelKey());

            traceRepository.recordStageCompleted(
                    runId, 1, "composer", composerSession.sessionId(), composerSession.finalOutputJson(), Instant.now());
            return new LessonGenerationPipelineResult(composerSession.finalOutputJson(), composerSession.sessionId(), runId);
        } catch (LessonGenerationFailedException ex) {
            traceRepository.markRunFailed(runId, ex.getMessage(), Instant.now());
            throw ex;
        } catch (RuntimeException ex) {
            traceRepository.markRunFailed(runId, failureMessage(ex), Instant.now());
            throw new LessonGenerationFailedException(runId, failureMessage(ex));
        }
    }

    private JsonNode runSourceNormalizationStage(
            UUID runId,
            LinkedHashMap<String, Object> input,
            String modelKey) {
        var session = runStageSession(
                runId,
                0,
                "source_normalization",
                SOURCE_NORMALIZER_PROFILE,
                "Normalize ordered lesson sources into a sourcePack JSON artifact.",
                input,
                sourceNormalizerPrompt(),
                modelKey);
        final JsonNode sourcePack;
        try {
            sourcePack = readJson(session.finalOutputJson());
            sourcePackValidator.validate(sourcePack);
        } catch (LessonContentValidationException ex) {
            traceRepository.recordStageFailed(
                    runId, 0, "source_normalization", session.sessionId(), ex.getMessage(), Instant.now());
            throw new LessonGenerationFailedException(session.sessionId(), ex.getMessage());
        }
        traceRepository.recordStageCompleted(
                runId, 0, "source_normalization", session.sessionId(), session.finalOutputJson(), Instant.now());
        return sourcePack;
    }

    private AgentSessionView runStageSession(
            UUID runId,
            int stageIndex,
            String stageKey,
            String profileKey,
            String task,
            LinkedHashMap<String, Object> input,
            String promptAppendix,
            String modelKey) {
        final AgentSessionView session;
        try {
            session = startAgentSessionUseCase.startSession(
                    new StartAgentSessionCommand(profileKey, modelKey, task, writeJson(input), promptAppendix, null));
        } catch (RuntimeException ex) {
            traceRepository.recordStageFailed(runId, stageIndex, stageKey, null, failureMessage(ex), Instant.now());
            throw new LessonGenerationFailedException(runId, failureMessage(ex));
        }

        if (!"COMPLETED".equals(session.status()) || session.finalOutputJson() == null) {
            var reason = session.failureReason() == null ? "stage did not produce final output" : session.failureReason();
            traceRepository.recordStageFailed(runId, stageIndex, stageKey, session.sessionId(), reason, Instant.now());
            throw new LessonGenerationFailedException(session.sessionId(), reason);
        }
        return session;
    }

    private LinkedHashMap<String, Object> baseInput(LessonGenerationPipelineRequest request) {
        var draftPayload = new LinkedHashMap<String, Object>();
        draftPayload.put("id", request.draft().id());
        draftPayload.put("title", request.draft().title());
        draftPayload.put("description", request.draft().description());
        draftPayload.put("userInstructions", request.draft().userInstructions());
        draftPayload.put("explanationLanguage", request.draft().explanationLanguage());
        draftPayload.put("translationLanguage", request.draft().translationLanguage());

        var input = new LinkedHashMap<String, Object>();
        input.put("draftId", request.draft().id());
        input.put("moduleKey", request.module().moduleKey());
        input.put("moduleSchemaVersion", request.module().schemaVersion());
        input.put("draft", draftPayload);
        return input;
    }

    private LinkedHashMap<String, Object> composerInput(LessonGenerationPipelineRequest request, JsonNode sourcePack) {
        var input = baseInput(request);
        input.put("sourcePack", sourcePack);
        return input;
    }

    private LinkedHashMap<String, Object> sourceProcessingPolicyPayload(LessonSourceProcessingPolicy policy) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("mode", policy.mode().name());
        payload.put("pdfHandlingMode", policy.pdfHandlingMode().name());
        payload.put("attachImagesToVisionStages", policy.attachImagesToVisionStages());
        payload.put("maxInlineImageBytes", policy.maxInlineImageBytes());
        payload.put("maxPdfRenderedPages", policy.maxPdfRenderedPages());
        return payload;
    }

    private String sourceNormalizerPrompt() {
        return """
                Return only a sourcePack JSON object with sourcePackVersion, sources, combinedText, and sourceRefs.
                Use the ordered sourceBundle manifest as the source list.
                For TEXT_NOTE and text DOCUMENT_FILE items, use textContent as normalizedText.
                For attached image parts, transcribe the visible Chinese textbook content into normalizedText.
                For PDF items with textContent, use that extracted text. For PDF items without textContent, use the rendered page image parts attached by source order.
                If a PDF item has no textContent and no rendered page images are attached, keep normalizedText empty and add a warning.
                Never return raw file bytes, base64, or contentBase64.
                """;
    }

    private String composerPrompt() {
        return """
                Return only the final hsk5_v2 lesson JSON object.
                Use the supplied sourcePack exactly; do not add raw sourceBundle, file bytes, base64, or contentBase64.
                This source-layer implementation may create a compact source_pack_summary section and one text section from sourcePack.combinedText.
                """;
    }

    private JsonNode readJson(String rawJson) {
        try {
            return objectMapper.readTree(rawJson);
        } catch (JsonProcessingException ex) {
            throw new LessonContentValidationException("Stage output must be valid JSON");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize hsk5_v2 source pipeline input", ex);
        }
    }

    private String failureMessage(RuntimeException ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank()
                ? "hsk5_v2 source pipeline failed"
                : ex.getMessage();
    }
}
