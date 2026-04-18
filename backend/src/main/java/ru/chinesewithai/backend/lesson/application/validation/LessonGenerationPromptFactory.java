package ru.chinesewithai.backend.lesson.application.validation;

import java.util.StringJoiner;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.lesson.domain.model.LessonModule;

@Component
public class LessonGenerationPromptFactory {

    private final LessonModuleStrategyCatalog strategyCatalog;

    public LessonGenerationPromptFactory(LessonModuleStrategyCatalog strategyCatalog) {
        this.strategyCatalog = strategyCatalog;
    }

    public String buildSystemPromptAppendix(LessonModule module) {
        var strategy = strategyCatalog.getRequired(module.moduleKey());
        var joiner = new StringJoiner("\n\n");
        joiner.add(module.systemPromptAppendix());
        joiner.add("""
                Lesson JSON contract:
                - Return one JSON object only.
                - Keep the top-level fields exactly as required by the output contract.
                - Keep studyLanguage as the language of the learning material.
                - Use explanationLanguage for titles, explanations, and study instructions.
                - Use translationLanguage for translation fields.
                - newWords must always be present as an array.
                - reviewWords must always be present as an array.
                - sections must always be present as an array.
                """);
        joiner.add(strategy.generationInstructions());
        return joiner.toString();
    }
}
