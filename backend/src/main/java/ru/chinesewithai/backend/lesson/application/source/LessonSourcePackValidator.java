package ru.chinesewithai.backend.lesson.application.source;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.lesson.application.exception.LessonContentValidationException;

@Component
public class LessonSourcePackValidator {

    public void validate(JsonNode sourcePack) {
        requireObject(sourcePack, "sourcePack");
        var version = sourcePack.path("sourcePackVersion");
        if (!version.canConvertToInt() || version.asInt() <= 0) {
            throw new LessonContentValidationException("sourcePack.sourcePackVersion must be a positive integer");
        }
        var sources = requireArray(sourcePack.get("sources"), "sourcePack.sources");
        if (sources.isEmpty()) {
            throw new LessonContentValidationException("sourcePack.sources must not be empty");
        }
        for (int i = 0; i < sources.size(); i++) {
            var source = requireObject(sources.get(i), "sourcePack.sources[" + i + "]");
            requireText(source.get("sourceId"), "sourcePack.sources[" + i + "].sourceId");
            requireText(source.get("mediaCategory"), "sourcePack.sources[" + i + "].mediaCategory");
            if (!source.path("position").canConvertToInt()) {
                throw new LessonContentValidationException("sourcePack.sources[" + i + "].position must be an integer");
            }
            var normalizedText = source.get("normalizedText");
            if (normalizedText != null && !normalizedText.isNull() && !normalizedText.isTextual()) {
                throw new LessonContentValidationException(
                        "sourcePack.sources[" + i + "].normalizedText must be a string when present");
            }
            var warnings = source.get("warnings");
            if (warnings != null && !warnings.isNull() && !warnings.isArray()) {
                throw new LessonContentValidationException(
                        "sourcePack.sources[" + i + "].warnings must be an array when present");
            }
        }
        var combinedText = sourcePack.get("combinedText");
        if (combinedText != null && !combinedText.isNull() && !combinedText.isTextual()) {
            throw new LessonContentValidationException("sourcePack.combinedText must be a string when present");
        }
        var sourceRefs = sourcePack.get("sourceRefs");
        if (sourceRefs != null && !sourceRefs.isNull() && !sourceRefs.isArray()) {
            throw new LessonContentValidationException("sourcePack.sourceRefs must be an array when present");
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
        if (node == null || !node.isTextual() || node.asText().trim().isBlank()) {
            throw new LessonContentValidationException(fieldName + " must be a non-empty string");
        }
        return node.asText().trim();
    }
}
