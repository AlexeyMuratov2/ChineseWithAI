package ru.chinesewithai.backend.storedfile.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "file_upload_sessions")
public class FileUploadSessionJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "state", nullable = false, length = 40)
    private String state;

    @Column(name = "upload_scenario", nullable = false, length = 64)
    private String uploadScenario;

    @Column(name = "bytes_received", nullable = false)
    private long bytesReceived;

    @Column(name = "bytes_expected")
    private Long bytesExpected;

    @Column(name = "percent")
    private Integer percent;

    @Column(name = "declared_content_type", length = 255)
    private String declaredContentType;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "result_file_id")
    private UUID resultFileId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FileUploadSessionJpaEntity() {}

    public FileUploadSessionJpaEntity(
            UUID id,
            String state,
            String uploadScenario,
            long bytesReceived,
            Long bytesExpected,
            Integer percent,
            String declaredContentType,
            String originalFileName,
            String errorMessage,
            UUID resultFileId,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.state = state;
        this.uploadScenario = uploadScenario;
        this.bytesReceived = bytesReceived;
        this.bytesExpected = bytesExpected;
        this.percent = percent;
        this.declaredContentType = declaredContentType;
        this.originalFileName = originalFileName;
        this.errorMessage = errorMessage;
        this.resultFileId = resultFileId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }

    public String getUploadScenario() {
        return uploadScenario;
    }

    public long getBytesReceived() {
        return bytesReceived;
    }

    public void setBytesReceived(long bytesReceived) {
        this.bytesReceived = bytesReceived;
    }

    public Long getBytesExpected() {
        return bytesExpected;
    }

    public Integer getPercent() {
        return percent;
    }

    public void setPercent(Integer percent) {
        this.percent = percent;
    }

    public String getDeclaredContentType() {
        return declaredContentType;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public UUID getResultFileId() {
        return resultFileId;
    }

    public void setResultFileId(UUID resultFileId) {
        this.resultFileId = resultFileId;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
