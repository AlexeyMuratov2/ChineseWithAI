package ru.chinesewithai.backend.lesson.application.generation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.agentruntime.application.command.StartAgentSessionCommand;
import ru.chinesewithai.backend.agentruntime.application.port.in.StartAgentSessionUseCase;
import ru.chinesewithai.backend.agentruntime.application.view.AgentSessionView;
import ru.chinesewithai.backend.lesson.application.exception.LessonContentValidationException;
import ru.chinesewithai.backend.lesson.application.exception.LessonGenerationFailedException;
import ru.chinesewithai.backend.lesson.application.port.out.LessonGenerationTraceRepository;
import ru.chinesewithai.backend.lesson.application.validation.Hsk5LessonArtifactValidator;
import ru.chinesewithai.backend.lesson.application.validation.LessonGenerationPromptFactory;

@Component
public class Hsk5QualityLessonGenerationPipeline implements LessonGenerationPipeline {

    public static final String PIPELINE_KEY = "hsk5-quality:v1";

    private static final String BLUEPRINT_PROFILE = "lesson-stage:hsk5_v1_blueprint";
    private static final String GRAMMAR_PROFILE = "lesson-stage:hsk5_v1_grammar";
    private static final String VOCABULARY_PRACTICE_PROFILE = "lesson-stage:hsk5_v1_vocabulary_practice";
    private static final String WORD_GAME_PROFILE = "lesson-stage:hsk5_v1_word_game";
    private static final String COMPOSER_PROFILE = "lesson-generator:hsk5_v1_composer";

    private final StartAgentSessionUseCase startAgentSessionUseCase;
    private final LessonGenerationTraceRepository traceRepository;
    private final LessonGenerationInputFactory inputFactory;
    private final LessonGenerationPromptFactory promptFactory;
    private final Hsk5LessonArtifactValidator artifactValidator;
    private final ObjectMapper objectMapper;

    public Hsk5QualityLessonGenerationPipeline(
            StartAgentSessionUseCase startAgentSessionUseCase,
            LessonGenerationTraceRepository traceRepository,
            LessonGenerationInputFactory inputFactory,
            LessonGenerationPromptFactory promptFactory,
            Hsk5LessonArtifactValidator artifactValidator,
            ObjectMapper objectMapper) {
        this.startAgentSessionUseCase = startAgentSessionUseCase;
        this.traceRepository = traceRepository;
        this.inputFactory = inputFactory;
        this.promptFactory = promptFactory;
        this.artifactValidator = artifactValidator;
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
            var sourceText = sourceText(request);
            var baseInput = inputFactory.build(request.draft(), request.module());
            baseInput.put("sourceText", sourceText);

            var blueprint = runStage(
                    runId,
                    0,
                    "blueprint",
                    BLUEPRINT_PROFILE,
                    "Create the hsk5_v1 lesson blueprint artifact.",
                    stageInput(baseInput, "blueprint"),
                    blueprintPrompt(),
                    request.modelKey(),
                    artifact -> artifactValidator.validateBlueprint(artifact, sourceText));

            var grammar = runStage(
                    runId,
                    1,
                    "grammar",
                    GRAMMAR_PROFILE,
                    "Create grammar sections for the hsk5_v1 lesson.",
                    stageInput(baseInput, "grammar", "blueprint", blueprint),
                    grammarPrompt(),
                    request.modelKey(),
                    artifactValidator::validateGrammarArtifact);

            var vocabularyPractice = runStage(
                    runId,
                    2,
                    "vocabulary_practice",
                    VOCABULARY_PRACTICE_PROFILE,
                    "Create word_study sections for the hsk5_v1 lesson.",
                    stageInput(baseInput, "vocabulary_practice", "blueprint", blueprint),
                    vocabularyPracticePrompt(),
                    request.modelKey(),
                    artifact -> artifactValidator.validateVocabularyPracticeArtifact(artifact, blueprint));

            var wordGame = runStage(
                    runId,
                    3,
                    "word_game",
                    WORD_GAME_PROFILE,
                    "Create the word game section for the hsk5_v1 lesson.",
                    stageInput(
                            baseInput,
                            "word_game",
                            "blueprint",
                            blueprint,
                            "vocabularyPractice",
                            vocabularyPractice),
                    wordGamePrompt(),
                    request.modelKey(),
                    artifact -> artifactValidator.validateWordGameArtifact(artifact, blueprint));

            var composerSession = runStageSession(
                    runId,
                    4,
                    "composer",
                    COMPOSER_PROFILE,
                    "Compose the final hsk5_v1 lesson JSON from the stage artifacts.",
                    stageInput(
                            baseInput,
                            "composer",
                            "blueprint",
                            blueprint,
                            "grammar",
                            grammar,
                            "vocabularyPractice",
                            vocabularyPractice,
                            "wordGame",
                            wordGame),
                    composerPrompt(request),
                    request.modelKey());

            traceRepository.recordStageCompleted(
                    runId, 4, "composer", composerSession.sessionId(), composerSession.finalOutputJson(), Instant.now());
            return new LessonGenerationPipelineResult(composerSession.finalOutputJson(), composerSession.sessionId(), runId);
        } catch (LessonGenerationFailedException ex) {
            traceRepository.markRunFailed(runId, ex.getMessage(), Instant.now());
            throw ex;
        } catch (RuntimeException ex) {
            traceRepository.markRunFailed(runId, failureMessage(ex), Instant.now());
            throw new LessonGenerationFailedException(runId, failureMessage(ex));
        }
    }

    private JsonNode runStage(
            UUID runId,
            int stageIndex,
            String stageKey,
            String profileKey,
            String task,
            LinkedHashMap<String, Object> input,
            String promptAppendix,
            String modelKey,
            Consumer<JsonNode> validator) {
        var session = runStageSession(runId, stageIndex, stageKey, profileKey, task, input, promptAppendix, modelKey);
        final JsonNode artifact;
        try {
            artifact = readJson(session.finalOutputJson());
            validator.accept(artifact);
        } catch (LessonContentValidationException ex) {
            traceRepository.recordStageFailed(runId, stageIndex, stageKey, session.sessionId(), ex.getMessage(), Instant.now());
            throw new LessonGenerationFailedException(session.sessionId(), ex.getMessage());
        }
        traceRepository.recordStageCompleted(
                runId, stageIndex, stageKey, session.sessionId(), session.finalOutputJson(), Instant.now());
        return artifact;
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

    private LinkedHashMap<String, Object> stageInput(LinkedHashMap<String, Object> baseInput, String stage) {
        var input = new LinkedHashMap<String, Object>(baseInput);
        input.put("stage", stage);
        return input;
    }

    private LinkedHashMap<String, Object> stageInput(
            LinkedHashMap<String, Object> baseInput, String stage, String firstKey, Object firstValue) {
        var input = stageInput(baseInput, stage);
        input.put(firstKey, firstValue);
        return input;
    }

    private LinkedHashMap<String, Object> stageInput(
            LinkedHashMap<String, Object> baseInput,
            String stage,
            String firstKey,
            Object firstValue,
            String secondKey,
            Object secondValue) {
        var input = stageInput(baseInput, stage, firstKey, firstValue);
        input.put(secondKey, secondValue);
        return input;
    }

    private LinkedHashMap<String, Object> stageInput(
            LinkedHashMap<String, Object> baseInput,
            String stage,
            String firstKey,
            Object firstValue,
            String secondKey,
            Object secondValue,
            String thirdKey,
            Object thirdValue,
            String fourthKey,
            Object fourthValue) {
        var input = stageInput(baseInput, stage, firstKey, firstValue, secondKey, secondValue);
        input.put(thirdKey, thirdValue);
        input.put(fourthKey, fourthValue);
        return input;
    }

    private String blueprintPrompt() {
        return """
                Return only a blueprint JSON object with title, readingText, newWords, reviewWords, grammarPoints, lessonTone, and lessonGoal.
                readingText must exactly equal sourceText. newWords and reviewWords use {word,pinyin,translation}.
                grammarPoints must contain at least one {name,pattern} item suitable for HSK 5.
                """;
    }

    private String grammarPrompt() {
        return """
                Return only a grammar artifact JSON object: {"grammarSections":[...]}.
                Each grammar section must already match the final lesson grammar block contract.
                """;
    }

    private String vocabularyPracticePrompt() {
        return """
                Return only a vocabulary practice artifact JSON object: {"sections":[...]}.
                Every section must be a final lesson word_study block for one blueprint new or review word.
                """;
    }

    private String wordGamePrompt() {
        return """
                Return only a word game artifact JSON object: {"section":{...}}.
                The section must be a final lesson word_game block and every round.answerWord must come from blueprint vocabulary.
                """;
    }

    private String composerPrompt(LessonGenerationPipelineRequest request) {
        return promptFactory.buildSystemPromptAppendix(request.module())
                + "\n\nCompose the final lesson by using the supplied blueprint, grammar, vocabularyPractice, and wordGame artifacts."
                + " Include the exact sourceText in the text block and include at least one grammar block.";
    }

    private String sourceText(LessonGenerationPipelineRequest request) {
        return request.draft().sources().getFirst().textContent().trim();
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
            throw new IllegalStateException("Failed to serialize lesson generation stage input", ex);
        }
    }

    private String failureMessage(RuntimeException ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank()
                ? "Lesson generation pipeline failed"
                : ex.getMessage();
    }
}
