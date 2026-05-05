package ru.chinesewithai.backend.lesson.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import ru.chinesewithai.backend.lesson.domain.model.LanguageTag;
import ru.chinesewithai.backend.lesson.domain.model.Lesson;
import ru.chinesewithai.backend.lesson.domain.model.LessonId;
import ru.chinesewithai.backend.lesson.domain.model.LessonModule;

@Component
public class LessonPersistenceMapper {

    private final ObjectMapper objectMapper;

    public LessonPersistenceMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public LessonJpaEntity toEntity(Lesson lesson) {
        return new LessonJpaEntity(
                lesson.id().value(),
                lesson.moduleKey(),
                lesson.sourceDraftId(),
                lesson.generatorSessionId(),
                lesson.title(),
                lesson.studyLanguage().value(),
                lesson.explanationLanguage().value(),
                lesson.translationLanguage().value(),
                readJson(lesson.contentJson()),
                lesson.createdAt(),
                lesson.updatedAt(),
                lesson.version());
    }

    public Lesson toDomain(LessonJpaEntity entity) {
        return Lesson.reconstitute(
                new LessonId(entity.getId()),
                entity.getModuleKey(),
                entity.getSourceDraftId(),
                entity.getGeneratorSessionId(),
                entity.getTitle(),
                LanguageTag.of(entity.getStudyLanguage()),
                LanguageTag.of(entity.getExplanationLanguage()),
                LanguageTag.of(entity.getTranslationLanguage()),
                writeJson(entity.getContentJson()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion());
    }

    public LessonModule toDomain(LessonModuleJpaEntity entity) {
        return new LessonModule(
                entity.getModuleKey(),
                entity.getDisplayName(),
                entity.getSystemPromptAppendix(),
                entity.getSchemaVersion(),
                entity.isActive(),
                entity.getGeneratorProfileKey(),
                entity.getGeneratorWorkflowVariantKey(),
                entity.getGenerationPipelineKey(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private com.fasterxml.jackson.databind.JsonNode readJson(String rawJson) {
        try {
            return objectMapper.readTree(rawJson);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse lesson content JSON", ex);
        }
    }

    private String writeJson(com.fasterxml.jackson.databind.JsonNode jsonNode) {
        try {
            return objectMapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize lesson content JSON", ex);
        }
    }
}
