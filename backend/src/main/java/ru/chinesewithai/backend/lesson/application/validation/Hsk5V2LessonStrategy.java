package ru.chinesewithai.backend.lesson.application.validation;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.lesson.application.exception.LessonContentValidationException;
import ru.chinesewithai.backend.lesson.application.source.LessonSourceProcessingPolicies;
import ru.chinesewithai.backend.lesson.domain.model.LessonModule;
import ru.chinesewithai.backend.lessondraft.application.view.LessonDraftView;

@Component
public class Hsk5V2LessonStrategy implements LessonModuleStrategy {

    public static final String MODULE_KEY = "hsk5_v2";

    private static final Set<String> ALLOWED_SECTION_TYPES = Set.of("source_pack_summary", "text");

    @Override
    public String moduleKey() {
        return MODULE_KEY;
    }

    @Override
    public void validateDraftForGeneration(LessonDraftView draft) {
        var policy = LessonSourceProcessingPolicies.hsk5V2NormalizeFirst();
        if (draft.sources().size() < policy.minSources() || draft.sources().size() > policy.maxSources()) {
            throw new LessonContentValidationException(
                    "hsk5_v2 requires between %d and %d draft sources".formatted(policy.minSources(), policy.maxSources()));
        }
        for (var source : draft.sources()) {
            if (!policy.allowedSourceTypes().contains(source.type())) {
                throw new LessonContentValidationException("hsk5_v2 source type is not supported: " + source.type());
            }
            if ("TEXT_NOTE".equals(source.type()) && (source.textContent() == null || source.textContent().isBlank())) {
                throw new LessonContentValidationException("hsk5_v2 requires non-empty textContent for TEXT_NOTE");
            }
            if ("DOCUMENT_FILE".equals(source.type()) && source.documentFileId() == null) {
                throw new LessonContentValidationException("hsk5_v2 requires documentFileId for DOCUMENT_FILE");
            }
        }
    }

    @Override
    public void validateLesson(JsonNode lessonJson, LessonModule module) {
        validateSourcePack(requireObject(lessonJson.get("sourcePack"), "sourcePack"));
        var sections = requireArray(lessonJson.get("sections"), "sections");
        if (sections.isEmpty()) {
            throw new LessonContentValidationException("hsk5_v2 sections must not be empty");
        }
        for (int i = 0; i < sections.size(); i++) {
            var section = requireObject(sections.get(i), "sections[" + i + "]");
            var type = requireText(section.get("type"), "sections[" + i + "].type");
            if (!ALLOWED_SECTION_TYPES.contains(type)) {
                throw new LessonContentValidationException("sections[" + i + "].type has unsupported hsk5_v2 block type");
            }
        }
    }

    @Override
    public String generationInstructions() {
        return """
                hsk5_v2 source-layer rules:
                - This module accepts multiple ordered sources.
                - Source normalization must happen before lesson composition.
                - The final lesson JSON must include sourcePack exactly as supplied to the composer.
                - Do not include raw file bytes or contentBase64 in final lesson JSON.
                - sections may contain source_pack_summary and text blocks while the full HSK5 v2 lesson contract is still being built.
                """;
    }

    private void validateSourcePack(JsonNode sourcePack) {
        if (!sourcePack.path("sourcePackVersion").canConvertToInt() || sourcePack.path("sourcePackVersion").asInt() <= 0) {
            throw new LessonContentValidationException("sourcePack.sourcePackVersion must be a positive integer");
        }
        var sources = requireArray(sourcePack.get("sources"), "sourcePack.sources");
        if (sources.isEmpty()) {
            throw new LessonContentValidationException("sourcePack.sources must not be empty");
        }
    }

    private static JsonNode requireObject(JsonNode node, String fieldName) {
        if (node == null || !node.isObject()) {
            throw new LessonContentValidationException(fieldName + " must be an object");
        }
        return node;
    }

    private static JsonNode requireArray(JsonNode node, String fieldName) {
        if (node == null || !node.isArray()) {
            throw new LessonContentValidationException(fieldName + " must be an array");
        }
        return node;
    }

    private static String requireText(JsonNode node, String fieldName) {
        if (node == null || !node.isTextual()) {
            throw new LessonContentValidationException(fieldName + " must be a string");
        }
        var value = node.asText().trim();
        if (value.isBlank()) {
            throw new LessonContentValidationException(fieldName + " must not be blank");
        }
        return value;
    }
}
