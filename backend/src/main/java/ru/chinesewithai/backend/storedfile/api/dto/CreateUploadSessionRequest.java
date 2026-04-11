package ru.chinesewithai.backend.storedfile.api.dto;

import jakarta.validation.constraints.Size;
import ru.chinesewithai.backend.storedfile.application.security.UploadScenario;

/**
 * Client declares expected size (for percentage) and optional hints; authoritative bytes still come
 * from the follow-up POST body.
 */
public record CreateUploadSessionRequest(
        UploadScenario scenario,
        Long expectedContentLength,
        @Size(max = 255) String declaredContentType,
        @Size(max = 255) String originalFileName) {}
