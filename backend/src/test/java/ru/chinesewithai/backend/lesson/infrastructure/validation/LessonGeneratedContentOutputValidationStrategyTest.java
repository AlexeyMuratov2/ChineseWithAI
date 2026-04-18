package ru.chinesewithai.backend.lesson.infrastructure.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import ru.chinesewithai.backend.agentruntime.application.port.out.OutputValidationStrategyRequest;
import ru.chinesewithai.backend.lesson.application.port.out.LessonModuleRepository;
import ru.chinesewithai.backend.lesson.application.validation.LessonContentValidator;
import ru.chinesewithai.backend.lesson.application.validation.LessonModuleStrategyCatalog;
import ru.chinesewithai.backend.lesson.application.validation.TestModuleLessonStrategy;
import ru.chinesewithai.backend.lesson.domain.model.LessonModule;

class LessonGeneratedContentOutputValidationStrategyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LessonModule module = new LessonModule(
            "TestModule",
            "TestModule",
            "prompt",
            1,
            true,
            Instant.now(),
            Instant.now());

    @Test
    void mapsLessonValidationFailuresToStructuredIssuesWithPaths() throws Exception {
        var strategy = new LessonGeneratedContentOutputValidationStrategy(
                new LessonContentValidator(
                        objectMapper, new LessonModuleStrategyCatalog(List.of(new TestModuleLessonStrategy()))),
                new LessonModuleRepository() {
                    @Override
                    public Optional<LessonModule> findByModuleKey(String moduleKey) {
                        return Optional.of(module);
                    }

                    @Override
                    public List<LessonModule> findAllOrderByModuleKeyAsc() {
                        return List.of();
                    }
                },
                objectMapper);
        var rawOutput = """
                {
                  "schemaVersion": 1,
                  "moduleKey": "TestModule",
                  "title": "Broken",
                  "studyLanguage": "zh",
                  "explanationLanguage": "zh",
                  "translationLanguage": "en",
                  "newWords": [
                    { "word": "词", "pinyin": "ci", "translation": "word" }
                  ],
                  "sections": [
                    {
                      "type": "word_usage",
                      "title": "Words",
                      "items": [
                        { "word": "词", "sentence": "句子", "translation": "Sentence" }
                      ]
                    },
                    {
                      "type": "reading",
                      "title": "Reading",
                      "translation": "Missing text"
                    }
                  ]
                }
                """;

        var issues = strategy.validate(new OutputValidationStrategyRequest(
                "lesson-generator:v1",
                "{\"moduleKey\":\"TestModule\"}",
                objectMapper.readTree(rawOutput),
                rawOutput));

        assertThat(issues).hasSize(1);
        assertThat(issues.getFirst().path()).isEqualTo("sections[1].text");
        assertThat(issues.getFirst().code()).isEqualTo("invalid_type");
        assertThat(issues.getFirst().message()).contains("sections[1].text");
    }
}
