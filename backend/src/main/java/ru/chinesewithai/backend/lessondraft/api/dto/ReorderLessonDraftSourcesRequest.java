package ru.chinesewithai.backend.lessondraft.api.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record ReorderLessonDraftSourcesRequest(@NotEmpty List<UUID> sourceIds) {}
