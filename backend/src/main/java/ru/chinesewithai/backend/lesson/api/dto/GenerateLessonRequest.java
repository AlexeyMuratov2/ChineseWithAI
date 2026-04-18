package ru.chinesewithai.backend.lesson.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record GenerateLessonRequest(
        UUID draftId, @NotBlank @Size(max = 120) String moduleKey, @Size(max = 120) String modelKey) {}
