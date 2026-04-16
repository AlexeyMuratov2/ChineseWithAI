package ru.chinesewithai.backend.agentruntime.application.port.in;

import java.util.List;
import ru.chinesewithai.backend.agentruntime.application.view.AgentProfileSummaryView;

public interface ListAgentProfilesUseCase {
    List<AgentProfileSummaryView> listProfiles();
}
