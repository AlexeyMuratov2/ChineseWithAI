package ru.chinesewithai.backend.agentruntime.application.port.out;

import java.util.List;

public interface AgentModelGateway {
    String providerKey();

    List<AgentModelDescriptor> supportedModels();

    AgentModelResponse generate(AgentModelRequest request);
}
