package ru.chinesewithai.backend.lesson.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "lessons")
public class LessonJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = "module_key", length = 120)
    private String moduleKey;

    @Column(name = "source_draft_id")
    private UUID sourceDraftId;

    @Column(name = "generator_session_id")
    private UUID generatorSessionId;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "study_language", nullable = false, length = 35)
    private String studyLanguage;

    @Column(name = "explanation_language", nullable = false, length = 35)
    private String explanationLanguage;

    @Column(name = "translation_language", nullable = false, length = 35)
    private String translationLanguage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode contentJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected LessonJpaEntity() {}

    public LessonJpaEntity(
            UUID id,
            UUID ownerId,
            String moduleKey,
            UUID sourceDraftId,
            UUID generatorSessionId,
            String title,
            String studyLanguage,
            String explanationLanguage,
            String translationLanguage,
            JsonNode contentJson,
            Instant createdAt,
            Instant updatedAt,
            long version) {
        this.id = id;
        this.ownerId = ownerId;
        this.moduleKey = moduleKey;
        this.sourceDraftId = sourceDraftId;
        this.generatorSessionId = generatorSessionId;
        this.title = title;
        this.studyLanguage = studyLanguage;
        this.explanationLanguage = explanationLanguage;
        this.translationLanguage = translationLanguage;
        this.contentJson = contentJson;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getModuleKey() {
        return moduleKey;
    }

    public UUID getSourceDraftId() {
        return sourceDraftId;
    }

    public UUID getGeneratorSessionId() {
        return generatorSessionId;
    }

    public String getTitle() {
        return title;
    }

    public String getStudyLanguage() {
        return studyLanguage;
    }

    public String getExplanationLanguage() {
        return explanationLanguage;
    }

    public String getTranslationLanguage() {
        return translationLanguage;
    }

    public JsonNode getContentJson() {
        return contentJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
