package ru.chinesewithai.backend.lesson.application.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.chinesewithai.backend.lesson.domain.model.LessonModule;

class LessonGenerationPromptFactoryTest {

    @Test
    void combinesModulePromptContractAndStrategyRules() {
        var factory =
                new LessonGenerationPromptFactory(new LessonModuleStrategyCatalog(List.of(new TestModuleLessonStrategy())));
        var module = new LessonModule(
                "TestModule",
                "TestModule",
                "Module prompt",
                1,
                true,
                "lesson-generator:v1",
                "draft-generation-with-review:v1",
                null,
                Instant.now(),
                Instant.now());

        var appendix = factory.buildSystemPromptAppendix(module);

        assertThat(appendix).contains("Module prompt");
        assertThat(appendix).contains("Lesson JSON contract");
        assertThat(appendix).contains("reviewWords must always be present");
        assertThat(appendix).contains("TestModule rules");
    }

    @Test
    void hsk5AppendixNamesCanonicalNestedFields() {
        var factory =
                new LessonGenerationPromptFactory(new LessonModuleStrategyCatalog(List.of(new Hsk5V1LessonStrategy())));
        var module = new LessonModule(
                "hsk5_v1",
                "HSK 5 v1",
                "Module prompt",
                1,
                true,
                "lesson-generator:hsk5_v1",
                null,
                null,
                Instant.now(),
                Instant.now());

        var appendix = factory.buildSystemPromptAppendix(module);

        assertThat(appendix).contains("sentences");
        assertThat(appendix).contains("Do not use exampleSentences");
        assertThat(appendix).contains("answerWord");
        assertThat(appendix).contains("Do not use expectedWord");
    }
}
