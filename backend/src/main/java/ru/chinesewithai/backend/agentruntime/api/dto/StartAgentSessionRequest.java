package ru.chinesewithai.backend.agentruntime.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

public record StartAgentSessionRequest(
        @NotBlank String profileKey, @NotBlank String modelKey, @NotBlank String task, JsonNode input) {}
