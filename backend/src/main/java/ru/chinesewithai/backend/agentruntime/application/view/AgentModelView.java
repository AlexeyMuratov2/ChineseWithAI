package ru.chinesewithai.backend.agentruntime.application.view;

import java.util.List;

public record AgentModelView(String modelKey, String displayName, String providerKey, List<String> capabilities) {

    public AgentModelView {
        capabilities = List.copyOf(capabilities);
    }
}
