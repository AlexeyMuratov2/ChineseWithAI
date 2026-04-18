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
                Instant.now(),
                Instant.now());

        var appendix = factory.buildSystemPromptAppendix(module);

        assertThat(appendix).contains("Module prompt");
        assertThat(appendix).contains("Lesson JSON contract");
        assertThat(appendix).contains("TestModule rules");
    }
}
