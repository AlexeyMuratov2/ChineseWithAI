package ru.chinesewithai.backend.lessondraft.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "lesson_drafts")
public class LessonDraftJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "user_instructions")
    private String userInstructions;

    @Column(name = "explanation_language", nullable = false, length = 35)
    private String explanationLanguage;

    @Column(name = "translation_language", nullable = false, length = 35)
    private String translationLanguage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @OneToMany(mappedBy = "draft", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<LessonDraftSourceJpaEntity> sources = new ArrayList<>();

    protected LessonDraftJpaEntity() {}

    public LessonDraftJpaEntity(
            UUID id,
            String title,
            String description,
            String userInstructions,
            String explanationLanguage,
            String translationLanguage,
            Instant createdAt,
            Instant updatedAt,
            long version) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.userInstructions = userInstructions;
        this.explanationLanguage = explanationLanguage;
        this.translationLanguage = translationLanguage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public void replaceSources(List<LessonDraftSourceJpaEntity> newSources) {
        this.sources.clear();
        for (var source : newSources) {
            addSource(source);
        }
    }

    private void addSource(LessonDraftSourceJpaEntity source) {
        source.attachToDraft(this);
        this.sources.add(source);
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getUserInstructions() {
        return userInstructions;
    }

    public String getExplanationLanguage() {
        return explanationLanguage;
    }

    public String getTranslationLanguage() {
        return translationLanguage;
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

    public List<LessonDraftSourceJpaEntity> getSources() {
        return sources;
    }
}
