package ru.chinesewithai.backend.storedfile.application.port.out;

import java.util.Optional;
import java.util.UUID;
import ru.chinesewithai.backend.storedfile.application.security.UploadScenario;
import ru.chinesewithai.backend.storedfile.domain.model.FileUploadSessionId;
import ru.chinesewithai.backend.storedfile.domain.model.UploadSessionState;

public record FileUploadSessionSnapshot(
        FileUploadSessionId id,
        UploadSessionState state,
        long bytesReceived,
        Optional<Long> bytesExpected,
        Optional<Integer> percent,
        Optional<UUID> resultFileId,
        Optional<String> errorMessage,
        Optional<String> declaredContentType,
        Optional<String> originalFileName,
        UploadScenario uploadScenario) {}
