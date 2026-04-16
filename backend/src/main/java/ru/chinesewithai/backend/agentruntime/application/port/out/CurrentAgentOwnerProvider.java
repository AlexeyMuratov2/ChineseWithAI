package ru.chinesewithai.backend.agentruntime.application.port.out;

import java.util.UUID;

public interface CurrentAgentOwnerProvider {
    UUID getCurrentOwnerId();
}
