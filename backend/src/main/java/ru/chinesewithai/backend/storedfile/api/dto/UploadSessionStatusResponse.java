package ru.chinesewithai.backend.storedfile.api.dto;

import java.util.UUID;
import ru.chinesewithai.backend.storedfile.application.port.out.FileUploadSessionSnapshot;

public record UploadSessionStatusResponse(
        UUID sessionId,
        String state,
        long bytesReceived,
        Long bytesExpected,
        Integer percent,
        UUID resultFileId,
        String errorMessage) {

    public static UploadSessionStatusResponse from(FileUploadSessionSnapshot s) {
        return new UploadSessionStatusResponse(
                s.id().value(),
                s.state().name(),
                s.bytesReceived(),
                s.bytesExpected().orElse(null),
                s.percent().orElse(null),
                s.resultFileId().orElse(null),
                s.errorMessage().orElse(null));
    }
}
