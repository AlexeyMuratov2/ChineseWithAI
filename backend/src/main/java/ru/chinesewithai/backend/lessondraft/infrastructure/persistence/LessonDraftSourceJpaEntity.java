package ru.chinesewithai.backend.lessondraft.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import ru.chinesewithai.backend.lessondraft.domain.model.LessonDraftSourceType;

@Entity
@Table(name = "lesson_draft_sources")
public class LessonDraftSourceJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "draft_id", nullable = false)
    private LessonDraftJpaEntity draft;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private LessonDraftSourceType sourceType;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "text_content")
    private String textContent;

    @Column(name = "document_file_id")
    private UUID documentFileId;

    @Column(name = "document_original_file_name", length = 255)
    private String documentOriginalFileName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LessonDraftSourceJpaEntity() {}

    public LessonDraftSourceJpaEntity(
            UUID id,
            LessonDraftSourceType sourceType,
            int position,
            String textContent,
            UUID documentFileId,
            String documentOriginalFileName,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.sourceType = sourceType;
        this.position = position;
        this.textContent = textContent;
        this.documentFileId = documentFileId;
        this.documentOriginalFileName = documentOriginalFileName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    void attachToDraft(LessonDraftJpaEntity draft) {
        this.draft = draft;
    }

    public UUID getId() {
        return id;
    }

    public LessonDraftJpaEntity getDraft() {
        return draft;
    }

    public LessonDraftSourceType getSourceType() {
        return sourceType;
    }

    public int getPosition() {
        return position;
    }

    public String getTextContent() {
        return textContent;
    }

    public UUID getDocumentFileId() {
        return documentFileId;
    }

    public String getDocumentOriginalFileName() {
        return documentOriginalFileName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
