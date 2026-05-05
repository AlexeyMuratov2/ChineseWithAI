package ru.chinesewithai.backend.lesson.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "lesson_modules")
public class LessonModuleJpaEntity {

    @Id
    @Column(name = "module_key", nullable = false, length = 120)
    private String moduleKey;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @Column(name = "system_prompt_appendix", nullable = false)
    private String systemPromptAppendix;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "generator_profile_key", nullable = false, length = 120)
    private String generatorProfileKey;

    @Column(name = "generator_workflow_variant_key", length = 120)
    private String generatorWorkflowVariantKey;

    @Column(name = "generation_pipeline_key", length = 120)
    private String generationPipelineKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LessonModuleJpaEntity() {}

    public String getModuleKey() {
        return moduleKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSystemPromptAppendix() {
        return systemPromptAppendix;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public boolean isActive() {
        return active;
    }

    public String getGeneratorProfileKey() {
        return generatorProfileKey;
    }

    public String getGeneratorWorkflowVariantKey() {
        return generatorWorkflowVariantKey;
    }

    public String getGenerationPipelineKey() {
        return generationPipelineKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
