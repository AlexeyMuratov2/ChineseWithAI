package ru.chinesewithai.backend.agentruntime.api.dto;

import java.util.List;

public record AgentModelDescriptorResponse(
        String modelKey, String displayName, String providerKey, List<String> capabilities) {}
