package ru.chinesewithai.backend.agentruntime.application.port.out;

import java.util.List;
import java.util.Optional;

public interface AgentModelCatalog {
    Optional<AgentModelDescriptor> findByModelKey(String modelKey);

    List<AgentModelDescriptor> findVisibleModels();
}
