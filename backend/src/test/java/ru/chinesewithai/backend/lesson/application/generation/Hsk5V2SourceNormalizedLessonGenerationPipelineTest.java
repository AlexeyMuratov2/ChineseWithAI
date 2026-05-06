package ru.chinesewithai.backend.lesson.application.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.chinesewithai.backend.agentruntime.application.command.StartAgentSessionCommand;
import ru.chinesewithai.backend.agentruntime.application.port.in.StartAgentSessionUseCase;
import ru.chinesewithai.backend.agentruntime.application.view.AgentSessionView;
import ru.chinesewithai.backend.lesson.application.port.out.LessonGenerationTraceRepository;
import ru.chinesewithai.backend.lesson.application.source.LessonSourceBundleFactory;
import ru.chinesewithai.backend.lesson.application.source.LessonSourcePackNormalizer;
import ru.chinesewithai.backend.lesson.application.source.LessonSourcePackValidator;
import ru.chinesewithai.backend.lesson.application.source.LessonSourceProcessorCatalog;
import ru.chinesewithai.backend.lesson.application.source.LessonPdfTextExtractor;
import ru.chinesewithai.backend.lesson.application.source.NormalizeFirstLessonSourceProcessor;
import ru.chinesewithai.backend.lesson.domain.model.LessonModule;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftSourceView;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftView;
import ru.chinesewithai.backend.storedfile.application.api.StoredFileFacade;

class Hsk5V2SourceNormalizedLessonGenerationPipelineTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void normalizesSourcesBeforeComposerAndDoesNotForwardRawBundle() {
        var gateway = new StubStartAgentSessionUseCase(objectMapper);
        var trace = new StubTraceRepository();
        var pipeline = pipeline(gateway, trace);

        var result = pipeline.generate(new LessonGenerationPipelineRequest(module(), draft(), "fake-model"));

        assertThat(result.finalOutputJson()).contains("\"moduleKey\":\"hsk5_v2\"");
        assertThat(gateway.profileKeys()).containsExactly(
                "lesson-stage:hsk5_v2_source_normalizer",
                "lesson-generator:hsk5_v2_composer");
        assertThat(gateway.inputs().get(0).has("sourceBundle")).isTrue();
        assertThat(gateway.inputs().get(0).toString()).doesNotContain("contentBase64");
        assertThat(gateway.inputs().get(1).has("sourcePack")).isTrue();
        assertThat(gateway.inputs().get(1).has("sourceBundle")).isFalse();
        assertThat(gateway.inputs().get(1).toString()).doesNotContain("contentBase64");
        assertThat(trace.completedStages()).containsExactly("source_normalization", "composer");
    }

    private Hsk5V2SourceNormalizedLessonGenerationPipeline pipeline(
            StartAgentSessionUseCase startAgentSessionUseCase,
            LessonGenerationTraceRepository traceRepository) {
        var sourceBundleFactory = new LessonSourceBundleFactory(mock(StoredFileFacade.class), new LessonPdfTextExtractor());
        var sourceNormalizer = new LessonSourcePackNormalizer();
        return new Hsk5V2SourceNormalizedLessonGenerationPipeline(
                startAgentSessionUseCase,
                traceRepository,
                new LessonSourceProcessorCatalog(List.of(
                        new NormalizeFirstLessonSourceProcessor(sourceBundleFactory, sourceNormalizer))),
                new LessonSourcePackValidator(),
                objectMapper);
    }

    private LessonModule module() {
        return new LessonModule(
                "hsk5_v2",
                "HSK 5 v2",
                "Module prompt",
                1,
                true,
                "lesson-generator:hsk5_v2_composer",
                null,
                Hsk5V2SourceNormalizedLessonGenerationPipeline.PIPELINE_KEY,
                Instant.now(),
                Instant.now());
    }

    private LessonDraftView draft() {
        return new LessonDraftView(
                UUID.randomUUID(),
                "Draft",
                null,
                null,
                "ru",
                "ru",
                List.of(
                        new LessonDraftSourceView(
                                UUID.randomUUID(), "TEXT_NOTE", 0, "Source one", null, null, Instant.now(), Instant.now()),
                        new LessonDraftSourceView(
                                UUID.randomUUID(), "TEXT_NOTE", 1, "Source two", null, null, Instant.now(), Instant.now())),
                Instant.now(),
                Instant.now(),
                0L);
    }

    private static final class StubStartAgentSessionUseCase implements StartAgentSessionUseCase {

        private final ObjectMapper objectMapper;
        private final List<String> profileKeys = new ArrayList<>();
        private final List<JsonNode> inputs = new ArrayList<>();

        private StubStartAgentSessionUseCase(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public AgentSessionView startSession(StartAgentSessionCommand command) {
            profileKeys.add(command.profileKey());
            var input = read(command.inputJson());
            inputs.add(input);
            var sessionId = UUID.randomUUID();
            return new AgentSessionView(
                    sessionId,
                    command.profileKey(),
                    command.modelKey(),
                    command.task(),
                    command.workflowVariantKey(),
                    "COMPLETED",
                    command.inputJson(),
                    outputFor(command.profileKey(), input),
                    null,
                    Instant.now(),
                    Instant.now(),
                    Instant.now(),
                    Instant.now(),
                    List.of());
        }

        private String outputFor(String profileKey, JsonNode input) {
            if ("lesson-stage:hsk5_v2_source_normalizer".equals(profileKey)) {
                return write(Map.of(
                        "sourcePackVersion", 1,
                        "sources", List.of(
                                Map.of(
                                        "sourceId", input.path("sourceBundle").path("sources").get(0).path("sourceId").asText(),
                                        "position", 0,
                                        "mediaCategory", "text",
                                        "normalizedText", "Source one",
                                        "warnings", List.of()),
                                Map.of(
                                        "sourceId", input.path("sourceBundle").path("sources").get(1).path("sourceId").asText(),
                                        "position", 1,
                                        "mediaCategory", "text",
                                        "normalizedText", "Source two",
                                        "warnings", List.of())),
                        "combinedText", "Source one\n\nSource two",
                        "sourceRefs", List.of()));
            }
            return write(Map.of(
                    "schemaVersion", 1,
                    "moduleKey", "hsk5_v2",
                    "title", "Draft",
                    "studyLanguage", "zh",
                    "explanationLanguage", "ru",
                    "translationLanguage", "ru",
                    "newWords", List.of(),
                    "reviewWords", List.of(),
                    "sourcePack", toMap(input.path("sourcePack")),
                    "sections", List.of(Map.of("type", "source_pack_summary", "title", "Source pack"))));
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> toMap(JsonNode node) {
            return objectMapper.convertValue(node, Map.class);
        }

        private JsonNode read(String rawJson) {
            try {
                return objectMapper.readTree(rawJson);
            } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
                throw new IllegalStateException(ex);
            }
        }

        private String write(Object value) {
            try {
                return objectMapper.writeValueAsString(value);
            } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
                throw new IllegalStateException(ex);
            }
        }

        private List<String> profileKeys() {
            return List.copyOf(profileKeys);
        }

        private List<JsonNode> inputs() {
            return List.copyOf(inputs);
        }
    }

    private static final class StubTraceRepository implements LessonGenerationTraceRepository {

        private final UUID runId = UUID.randomUUID();
        private final List<String> completedStages = new ArrayList<>();

        @Override
        public UUID startRun(UUID draftId, String moduleKey, String pipelineKey, Instant now) {
            return runId;
        }

        @Override
        public void recordStageCompleted(
                UUID runId, int stageIndex, String stageKey, UUID agentSessionId, String outputJson, Instant finishedAt) {
            completedStages.add(stageKey);
        }

        @Override
        public void recordStageFailed(
                UUID runId, int stageIndex, String stageKey, UUID agentSessionId, String failureReason, Instant finishedAt) {}

        @Override
        public void markRunCompleted(UUID runId, UUID lessonId, UUID finalGeneratorSessionId, Instant finishedAt) {}

        @Override
        public void markRunFailed(UUID runId, String failureReason, Instant finishedAt) {}

        private List<String> completedStages() {
            return List.copyOf(completedStages);
        }
    }
}
