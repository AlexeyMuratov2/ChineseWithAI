package ru.chinesewithai.backend.storedfile.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.chinesewithai.backend.storedfile.application.port.out.FileUploadSessionRepository;
import ru.chinesewithai.backend.storedfile.application.port.out.FileUploadSessionSnapshot;
import ru.chinesewithai.backend.storedfile.application.security.UploadScenario;
import ru.chinesewithai.backend.storedfile.domain.model.FileUploadSessionId;
import ru.chinesewithai.backend.storedfile.domain.model.StoredFileId;
import ru.chinesewithai.backend.storedfile.domain.model.UploadSessionState;

@Component
class FileUploadSessionRepositoryJpaAdapter implements FileUploadSessionRepository {

    private final SpringDataFileUploadSessionJpaRepository jpa;

    FileUploadSessionRepositoryJpaAdapter(SpringDataFileUploadSessionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public void insert(
            FileUploadSessionId id,
            UploadScenario scenario,
            Optional<Long> bytesExpected,
            Optional<String> declaredContentType,
            Optional<String> originalFileName,
            Instant now) {
        var entity = new FileUploadSessionJpaEntity(
                id.value(),
                UploadSessionState.PENDING.name(),
                scenario.name(),
                0L,
                bytesExpected.orElse(null),
                null,
                declaredContentType.orElse(null),
                originalFileName.orElse(null),
                null,
                null,
                now,
                now);
        jpa.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FileUploadSessionSnapshot> find(FileUploadSessionId id) {
        return jpa.findById(id.value()).map(this::toSnapshot);
    }

    @Override
    @Transactional
    public void updateState(FileUploadSessionId id, UploadSessionState state, Instant now) {
        var entity = jpa.findById(id.value()).orElseThrow();
        entity.setState(state.name());
        entity.setUpdatedAt(now);
        jpa.save(entity);
    }

    @Override
    @Transactional
    public void updateProgress(FileUploadSessionId id, long bytesReceived, Optional<Integer> percent, Instant now) {
        var entity = jpa.findById(id.value()).orElseThrow();
        entity.setBytesReceived(bytesReceived);
        entity.setPercent(percent.orElse(null));
        entity.setUpdatedAt(now);
        jpa.save(entity);
    }

    @Override
    @Transactional
    public void complete(FileUploadSessionId id, StoredFileId resultFileId, Instant now) {
        var entity = jpa.findById(id.value()).orElseThrow();
        entity.setState(UploadSessionState.COMPLETED.name());
        entity.setResultFileId(resultFileId.value());
        entity.setPercent(100);
        entity.setUpdatedAt(now);
        jpa.save(entity);
    }

    @Override
    @Transactional
    public void fail(FileUploadSessionId id, String errorMessage, Instant now) {
        var entity = jpa.findById(id.value()).orElseThrow();
        entity.setState(UploadSessionState.FAILED.name());
        entity.setErrorMessage(errorMessage);
        entity.setUpdatedAt(now);
        jpa.save(entity);
    }

    private FileUploadSessionSnapshot toSnapshot(FileUploadSessionJpaEntity e) {
        return new FileUploadSessionSnapshot(
                new FileUploadSessionId(e.getId()),
                UploadSessionState.valueOf(e.getState()),
                e.getBytesReceived(),
                Optional.ofNullable(e.getBytesExpected()),
                Optional.ofNullable(e.getPercent()),
                Optional.ofNullable(e.getResultFileId()),
                Optional.ofNullable(e.getErrorMessage()),
                Optional.ofNullable(e.getDeclaredContentType()),
                Optional.ofNullable(e.getOriginalFileName()),
                UploadScenario.valueOf(e.getUploadScenario()));
    }
}
