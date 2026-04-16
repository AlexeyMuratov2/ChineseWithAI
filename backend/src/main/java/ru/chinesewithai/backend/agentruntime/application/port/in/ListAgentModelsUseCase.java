package ru.chinesewithai.backend.agentruntime.application.port.in;

import java.util.List;
import ru.chinesewithai.backend.agentruntime.application.view.AgentModelView;

public interface ListAgentModelsUseCase {
    List<AgentModelView> listModels();
}
