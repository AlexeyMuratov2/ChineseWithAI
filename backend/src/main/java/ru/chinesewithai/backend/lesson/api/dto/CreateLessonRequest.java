package ru.chinesewithai.backend.lesson.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateLessonRequest(@Size(max = 120) String moduleKey, UUID sourceDraftId, JsonNode content) {}
