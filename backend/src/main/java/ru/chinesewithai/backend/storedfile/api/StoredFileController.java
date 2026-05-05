package ru.chinesewithai.backend.storedfile.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import ru.chinesewithai.backend.storedfile.api.dto.CreateUploadSessionRequest;
import ru.chinesewithai.backend.storedfile.api.dto.CreateUploadSessionResponse;
import ru.chinesewithai.backend.storedfile.api.dto.StoredFileMetadataResponse;
import ru.chinesewithai.backend.storedfile.api.dto.UploadSessionStatusResponse;
import ru.chinesewithai.backend.storedfile.application.api.DeleteStoredFileResult;
import ru.chinesewithai.backend.storedfile.application.api.StoredFileFacade;
import ru.chinesewithai.backend.storedfile.application.command.CreateUploadSessionCommand;
import ru.chinesewithai.backend.storedfile.application.command.DeleteStoredFileCommand;
import ru.chinesewithai.backend.storedfile.application.security.UploadScenario;
import ru.chinesewithai.backend.storedfile.domain.model.FileUploadSessionId;
import ru.chinesewithai.backend.storedfile.domain.model.StoredFileId;

/**
 * HTTP adapter: validates input, maps DTOs, delegates to {@link StoredFileFacade}. No S3 or JPA
 * types here.
 */
@RestController
@RequestMapping("/api/v1/stored-files")
public class StoredFileController {

    public static final String HEADER_ORIGINAL_FILE_NAME = "X-Upload-Original-File-Name";

    private final StoredFileFacade storedFiles;

    public StoredFileController(StoredFileFacade storedFiles) {
        this.storedFiles = storedFiles;
    }

    @PostMapping("/upload-sessions")
    public ResponseEntity<CreateUploadSessionResponse> createUploadSession(
            @Valid @RequestBody CreateUploadSessionRequest request) {
        var scenario = request.scenario() == null ? UploadScenario.GENERIC_UPLOAD : request.scenario();
        var command = new CreateUploadSessionCommand(
                scenario,
                Optional.ofNullable(request.expectedContentLength()),
                Optional.ofNullable(request.declaredContentType()),
                Optional.ofNullable(request.originalFileName()));
        var id = storedFiles.createUploadSession(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateUploadSessionResponse(id.value()));
    }

    @GetMapping("/upload-sessions/{sessionId}")
    public ResponseEntity<UploadSessionStatusResponse> getUploadSession(@PathVariable UUID sessionId) {
        return storedFiles
                .getUploadSession(new FileUploadSessionId(sessionId))
                .map(s -> ResponseEntity.ok(UploadSessionStatusResponse.from(s)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/upload-sessions/{sessionId}/content")
    public ResponseEntity<StoredFileMetadataResponse> uploadContent(
            @PathVariable UUID sessionId,
            @RequestHeader(value = HttpHeaders.CONTENT_TYPE, required = false) String contentType,
            @RequestHeader(value = HEADER_ORIGINAL_FILE_NAME, required = false) String originalFileName,
            HttpServletRequest request)
            throws IOException {
        long length = request.getContentLengthLong();
        if (length < 0) {
            return ResponseEntity.status(HttpStatus.LENGTH_REQUIRED).build();
        }
        var meta = storedFiles.receiveSessionUpload(
                new FileUploadSessionId(sessionId),
                length,
                Optional.ofNullable(contentType),
                Optional.ofNullable(originalFileName),
                request.getInputStream());
        return ResponseEntity.ok(StoredFileMetadataResponse.from(meta));
    }

    @GetMapping("/{fileId}/content")
    public ResponseEntity<StreamingResponseBody> downloadContent(@PathVariable UUID fileId) {
        var contentOpt = storedFiles.openContent(StoredFileId.of(fileId));
        if (contentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var content = contentOpt.get();
        var ct = content.contentType().orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(ct));
        headers.setContentLength(content.sizeBytes());
        content.originalFileName()
                .ifPresent(name -> headers.setContentDispositionFormData("attachment", name));

        StreamingResponseBody body = outputStream -> {
            try (content) {
                content.inputStream().transferTo(outputStream);
            }
        };
        return ResponseEntity.ok().headers(headers).body(body);
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> delete(@PathVariable UUID fileId) {
        var result = storedFiles.delete(new DeleteStoredFileCommand(fileId));
        return switch (result) {
            case SUCCESS, ALREADY_ABSENT -> ResponseEntity.noContent().build();
            case STORAGE_FAILURE -> ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        };
    }

}
