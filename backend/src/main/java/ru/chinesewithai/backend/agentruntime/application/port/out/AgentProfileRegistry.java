package ru.chinesewithai.backend.agentruntime.application.port.out;

import java.util.List;
import java.util.Optional;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentProfile;

public interface AgentProfileRegistry {
    Optional<AgentProfile> findByProfileKey(String profileKey);

    List<AgentProfile> findVisibleProfiles();
}
