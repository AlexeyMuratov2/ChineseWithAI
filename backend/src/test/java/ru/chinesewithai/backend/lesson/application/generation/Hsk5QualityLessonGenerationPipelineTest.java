package ru.chinesewithai.backend.lesson.application.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

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
import ru.chinesewithai.backend.lesson.application.exception.LessonGenerationFailedException;
import ru.chinesewithai.backend.lesson.application.port.out.LessonGenerationTraceRepository;
import ru.chinesewithai.backend.lesson.application.validation.Hsk5LessonArtifactValidator;
import ru.chinesewithai.backend.lesson.application.validation.Hsk5V1LessonStrategy;
import ru.chinesewithai.backend.lesson.application.validation.LessonGenerationPromptFactory;
import ru.chinesewithai.backend.lesson.application.validation.LessonModuleStrategyCatalog;
import ru.chinesewithai.backend.lesson.domain.model.LessonModule;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftSourceView;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftView;
import ru.chinesewithai.backend.storedfile.application.api.StoredFileFacade;

class Hsk5QualityLessonGenerationPipelineTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void runsStagesInOrderAndHandsArtifactsForward() {
        var gateway = new StubStartAgentSessionUseCase(objectMapper, null);
        var trace = new StubTraceRepository();
        var pipeline = pipeline(gateway, trace);

        var result = pipeline.generate(new LessonGenerationPipelineRequest(module(), draft("source text"), "fake-model"));

        assertThat(result.finalOutputJson()).contains("\"moduleKey\":\"hsk5_v1\"");
        assertThat(gateway.profileKeys()).containsExactly(
                "lesson-stage:hsk5_v1_blueprint",
                "lesson-stage:hsk5_v1_grammar",
                "lesson-stage:hsk5_v1_vocabulary_practice",
                "lesson-stage:hsk5_v1_word_game",
                "lesson-generator:hsk5_v1_composer");
        assertThat(gateway.stageInputs()).containsExactly(
                "blueprint", "grammar", "vocabulary_practice", "word_game", "composer");
        assertThat(trace.completedStages()).containsExactly(
                "blueprint", "grammar", "vocabulary_practice", "word_game", "composer");
        assertThat(trace.failedRunReasons()).isEmpty();
    }

    @Test
    void stopsAndMarksRunFailedWhenStageFails() {
        var gateway = new StubStartAgentSessionUseCase(objectMapper, "lesson-stage:hsk5_v1_grammar");
        var trace = new StubTraceRepository();
        var pipeline = pipeline(gateway, trace);

        assertThatThrownBy(() -> pipeline.generate(new LessonGenerationPipelineRequest(
                        module(), draft("source text"), "fake-model")))
                .isInstanceOf(LessonGenerationFailedException.class)
                .hasMessageContaining("grammar failed");

        assertThat(gateway.profileKeys()).containsExactly(
                "lesson-stage:hsk5_v1_blueprint",
                "lesson-stage:hsk5_v1_grammar");
        assertThat(trace.completedStages()).containsExactly("blueprint");
        assertThat(trace.failedStages()).containsExactly("grammar");
        assertThat(trace.failedRunReasons()).singleElement().asString().contains("grammar failed");
    }

    @Test
    void recordsStageFailureWhenStageReturnsInvalidJson() {
        var gateway = new StubStartAgentSessionUseCase(objectMapper, null, "lesson-stage:hsk5_v1_grammar");
        var trace = new StubTraceRepository();
        var pipeline = pipeline(gateway, trace);

        assertThatThrownBy(() -> pipeline.generate(new LessonGenerationPipelineRequest(
                        module(), draft("source text"), "fake-model")))
                .isInstanceOf(LessonGenerationFailedException.class)
                .hasMessageContaining("Stage output must be valid JSON");

        assertThat(gateway.profileKeys()).containsExactly(
                "lesson-stage:hsk5_v1_blueprint",
                "lesson-stage:hsk5_v1_grammar");
        assertThat(trace.completedStages()).containsExactly("blueprint");
        assertThat(trace.failedStages()).containsExactly("grammar");
        assertThat(trace.failedRunReasons()).singleElement().asString().contains("Stage output must be valid JSON");
    }

    private Hsk5QualityLessonGenerationPipeline pipeline(
            StartAgentSessionUseCase startAgentSessionUseCase, LessonGenerationTraceRepository traceRepository) {
        return new Hsk5QualityLessonGenerationPipeline(
                startAgentSessionUseCase,
                traceRepository,
                new LessonGenerationInputFactory(mock(StoredFileFacade.class)),
                new LessonGenerationPromptFactory(new LessonModuleStrategyCatalog(List.of(new Hsk5V1LessonStrategy()))),
                new Hsk5LessonArtifactValidator(),
                objectMapper);
    }

    private LessonModule module() {
        return new LessonModule(
                "hsk5_v1",
                "HSK 5 v1",
                "Module prompt",
                1,
                true,
                "lesson-generator:hsk5_v1_composer",
                null,
                Hsk5QualityLessonGenerationPipeline.PIPELINE_KEY,
                Instant.now(),
                Instant.now());
    }

    private LessonDraftView draft(String text) {
        return new LessonDraftView(
                UUID.randomUUID(),
                "Draft",
                null,
                null,
                "ru",
                "ru",
                List.of(new LessonDraftSourceView(
                        UUID.randomUUID(), "TEXT_NOTE", 0, text, null, null, Instant.now(), Instant.now())),
                Instant.now(),
                Instant.now(),
                0L);
    }

    private static final class StubStartAgentSessionUseCase implements StartAgentSessionUseCase {

        private final ObjectMapper objectMapper;
        private final String failingProfileKey;
        private final String invalidOutputProfileKey;
        private final List<String> profileKeys = new ArrayList<>();
        private final List<String> stageInputs = new ArrayList<>();

        private StubStartAgentSessionUseCase(ObjectMapper objectMapper, String failingProfileKey) {
            this(objectMapper, failingProfileKey, null);
        }

        private StubStartAgentSessionUseCase(
                ObjectMapper objectMapper, String failingProfileKey, String invalidOutputProfileKey) {
            this.objectMapper = objectMapper;
            this.failingProfileKey = failingProfileKey;
            this.invalidOutputProfileKey = invalidOutputProfileKey;
        }

        @Override
        public AgentSessionView startSession(StartAgentSessionCommand command) {
            profileKeys.add(command.profileKey());
            var input = read(command.inputJson());
            stageInputs.add(input.path("stage").asText());
            var sessionId = UUID.randomUUID();
            if (command.profileKey().equals(failingProfileKey)) {
                return session(command, sessionId, "FAILED", null, "grammar failed");
            }
            if (command.profileKey().equals(invalidOutputProfileKey)) {
                return session(command, sessionId, "COMPLETED", "{not-json", null);
            }
            return session(command, sessionId, "COMPLETED", outputFor(command.profileKey(), input), null);
        }

        private AgentSessionView session(
                StartAgentSessionCommand command, UUID sessionId, String status, String output, String failureReason) {
            return new AgentSessionView(
                    sessionId,
                    command.profileKey(),
                    command.modelKey(),
                    command.task(),
                    command.workflowVariantKey(),
                    status,
                    command.inputJson(),
                    output,
                    failureReason,
                    Instant.now(),
                    Instant.now(),
                    Instant.now(),
                    Instant.now(),
                    List.of());
        }

        private String outputFor(String profileKey, com.fasterxml.jackson.databind.JsonNode input) {
            if ("lesson-stage:hsk5_v1_blueprint".equals(profileKey)) {
                return write(Map.of(
                        "title", "Draft",
                        "readingText", input.path("sourceText").asText(),
                        "newWords", List.of(word("机会")),
                        "reviewWords", List.of(word("影响")),
                        "grammarPoints", List.of(Map.of("name", "Although", "pattern", "虽然...,但是...")),
                        "lessonTone", "warm",
                        "lessonGoal", "practice"));
            }
            if ("lesson-stage:hsk5_v1_grammar".equals(profileKey)) {
                return write(Map.of("grammarSections", List.of(Map.of(
                        "type", "grammar",
                        "title", "Grammar",
                        "points", List.of(Map.of(
                                "name", "Although",
                                "pattern", "虽然...,但是...",
                                "explanation", "Contrast.",
                                "examples", List.of(Map.of("sentence", "句子", "translation", "Sentence")),
                                "exercises", List.of(Map.of("prompt", "Make a sentence."))))))));
            }
            if ("lesson-stage:hsk5_v1_vocabulary_practice".equals(profileKey)) {
                return write(Map.of("sections", List.of(wordStudy("机会", "new"), wordStudy("影响", "review"))));
            }
            if ("lesson-stage:hsk5_v1_word_game".equals(profileKey)) {
                return write(Map.of("section", Map.of(
                        "type", "word_game",
                        "title", "Game",
                        "instructions", "Guess",
                        "rounds", List.of(Map.of("prompt", "chance", "answerWord", "机会")))));
            }
            return write(Map.of(
                    "schemaVersion", 1,
                    "moduleKey", "hsk5_v1",
                    "title", "Draft",
                    "studyLanguage", "zh",
                    "explanationLanguage", "ru",
                    "translationLanguage", "ru",
                    "newWords", List.of(word("机会")),
                    "reviewWords", List.of(word("影响")),
                    "sections", List.of(
                            wordStudy("机会", "new"),
                            wordStudy("影响", "review"),
                            Map.of("type", "grammar", "title", "Grammar", "points", List.of(Map.of("name", "Although"))),
                            Map.of(
                                    "type",
                                    "text",
                                    "title",
                                    "Text",
                                    "text",
                                    input.path("sourceText").asText(),
                                    "readingPrompt",
                                    "Read",
                                    "discussionPrompts",
                                    List.of("Why?")),
                            Map.of(
                                    "type",
                                    "word_game",
                                    "title",
                                    "Game",
                                    "instructions",
                                    "Guess",
                                    "rounds",
                                    List.of(Map.of("prompt", "chance", "answerWord", "机会"))))));
        }

        private Map<String, Object> word(String value) {
            return Map.of("word", value, "pinyin", "pinyin", "translation", "translation");
        }

        private Map<String, Object> wordStudy(String value, String status) {
            return Map.of(
                    "type", "word_study",
                    "title", value,
                    "word", value,
                    "pinyin", "pinyin",
                    "translation", "translation",
                    "vocabularyStatus", status,
                    "sentences", List.of(Map.of("sentence", "句子", "translation", "Sentence")));
        }

        private com.fasterxml.jackson.databind.JsonNode read(String rawJson) {
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

        private List<String> stageInputs() {
            return List.copyOf(stageInputs);
        }
    }

    private static final class StubTraceRepository implements LessonGenerationTraceRepository {

        private final UUID runId = UUID.randomUUID();
        private final List<String> completedStages = new ArrayList<>();
        private final List<String> failedStages = new ArrayList<>();
        private final List<String> failedRunReasons = new ArrayList<>();

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
                UUID runId, int stageIndex, String stageKey, UUID agentSessionId, String failureReason, Instant finishedAt) {
            failedStages.add(stageKey);
        }

        @Override
        public void markRunCompleted(UUID runId, UUID lessonId, UUID finalGeneratorSessionId, Instant finishedAt) {}

        @Override
        public void markRunFailed(UUID runId, String failureReason, Instant finishedAt) {
            failedRunReasons.add(failureReason);
        }

        private List<String> completedStages() {
            return List.copyOf(completedStages);
        }

        private List<String> failedStages() {
            return List.copyOf(failedStages);
        }

        private List<String> failedRunReasons() {
            return List.copyOf(failedRunReasons);
        }
    }
}
