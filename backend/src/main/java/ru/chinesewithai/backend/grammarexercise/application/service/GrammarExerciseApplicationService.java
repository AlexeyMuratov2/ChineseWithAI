package ru.chinesewithai.backend.grammarexercise.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.stereotype.Service;
import ru.chinesewithai.backend.agentruntime.application.command.StartAgentSessionCommand;
import ru.chinesewithai.backend.agentruntime.application.port.in.StartAgentSessionUseCase;
import ru.chinesewithai.backend.grammarexercise.application.command.GenerateGrammarExerciseCommand;
import ru.chinesewithai.backend.grammarexercise.application.command.GrammarExerciseItemCommand;
import ru.chinesewithai.backend.grammarexercise.application.exception.GrammarExerciseGenerationFailedException;
import ru.chinesewithai.backend.grammarexercise.application.port.in.GenerateGrammarExerciseUseCase;
import ru.chinesewithai.backend.grammarexercise.application.view.GrammarExerciseView;
import ru.chinesewithai.backend.grammarexercise.infrastructure.config.GrammarExerciseGenerationProperties;

@Service
public class GrammarExerciseApplicationService implements GenerateGrammarExerciseUseCase {

    private static final String PROFILE_KEY = "grammar-exercise-generator:v1";
    private static final String GENERATE_TASK = "Generate a grammar exercise JSON from the provided grammar targets.";
    private static final String DEFAULT_EXPLANATION_LANGUAGE = "zh";

    private final StartAgentSessionUseCase startAgentSessionUseCase;
    private final GrammarExerciseGenerationProperties generationProperties;
    private final ObjectMapper objectMapper;

    public GrammarExerciseApplicationService(
            StartAgentSessionUseCase startAgentSessionUseCase,
            GrammarExerciseGenerationProperties generationProperties,
            ObjectMapper objectMapper) {
        this.startAgentSessionUseCase = startAgentSessionUseCase;
        this.generationProperties = generationProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public GrammarExerciseView generate(GenerateGrammarExerciseCommand command) {
        var session = startAgentSessionUseCase.startSession(new StartAgentSessionCommand(
                PROFILE_KEY,
                resolveModelKey(command.modelKey()),
                GENERATE_TASK,
                writeJson(buildGenerationInput(command)),
                null,
                null));

        if (!"COMPLETED".equals(session.status()) || session.finalOutputJson() == null) {
            throw new GrammarExerciseGenerationFailedException(session.sessionId(), session.failureReason());
        }

        return new GrammarExerciseView(session.sessionId(), readJson(session.finalOutputJson()));
    }

    private String resolveModelKey(String requestedModelKey) {
        var normalized = normalizeOptional(requestedModelKey);
        return normalized == null ? generationProperties.defaultModelKey() : normalized;
    }

    private Object buildGenerationInput(GenerateGrammarExerciseCommand command) {
        var input = new LinkedHashMap<String, Object>();
        input.put("explanationLanguage", resolveExplanationLanguage(command.explanationLanguage()));
        input.put("items", command.items().stream().map(this::toInputItem).toList());
        return input;
    }

    private String resolveExplanationLanguage(String requestedLanguage) {
        var normalized = normalizeOptional(requestedLanguage);
        return normalized == null ? DEFAULT_EXPLANATION_LANGUAGE : normalized;
    }

    private Object toInputItem(GrammarExerciseItemCommand item) {
        var inputItem = new LinkedHashMap<String, Object>();
        inputItem.put("term", normalizeRequired(item.term(), "term"));
        inputItem.put("focus", normalizeRequired(item.focus(), "focus"));
        return inputItem;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize grammar exercise generation input", ex);
        }
    }

    private JsonNode readJson(String rawJson) {
        try {
            return objectMapper.readTree(rawJson);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse generated grammar exercise JSON", ex);
        }
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        var normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
