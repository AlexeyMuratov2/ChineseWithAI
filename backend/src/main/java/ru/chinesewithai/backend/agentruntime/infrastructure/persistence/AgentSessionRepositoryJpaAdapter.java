package ru.chinesewithai.backend.agentruntime.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentSessionRepository;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentSession;

@Repository
public class AgentSessionRepositoryJpaAdapter implements AgentSessionRepository {

    private final SpringDataAgentSessionJpaRepository repository;

    public AgentSessionRepositoryJpaAdapter(SpringDataAgentSessionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public AgentSession save(AgentSession session) {
        return AgentRuntimeJpaMapper.toDomain(repository.save(AgentRuntimeJpaMapper.toEntity(session)));
    }

    @Override
    public Optional<AgentSession> findByIdAndOwnerId(UUID sessionId, UUID ownerId) {
        return repository.findByIdAndOwnerId(sessionId, ownerId).map(AgentRuntimeJpaMapper::toDomain);
    }
}
