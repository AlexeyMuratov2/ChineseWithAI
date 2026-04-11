package ru.chinesewithai.backend.storedfile.application.command;

import java.util.Optional;
import ru.chinesewithai.backend.storedfile.application.security.UploadScenario;

public record CreateUploadSessionCommand(
        UploadScenario scenario,
        Optional<Long> expectedContentLength,
        Optional<String> declaredContentType,
        Optional<String> originalFileName) {}
