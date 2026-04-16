package ru.chinesewithai.backend.agentruntime.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import ru.chinesewithai.backend.agentruntime.application.port.out.AgentStepRepository;
import ru.chinesewithai.backend.agentruntime.domain.model.AgentStep;

@Repository
public class AgentStepRepositoryJpaAdapter implements AgentStepRepository {

    private final SpringDataAgentStepJpaRepository repository;

    public AgentStepRepositoryJpaAdapter(SpringDataAgentStepJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public AgentStep save(AgentStep step) {
        return AgentRuntimeJpaMapper.toDomain(repository.save(AgentRuntimeJpaMapper.toEntity(step)));
    }

    @Override
    public List<AgentStep> findBySessionIdOrderByStepIndex(UUID sessionId) {
        return repository.findBySessionIdOrderByStepIndexAsc(sessionId).stream()
                .map(AgentRuntimeJpaMapper::toDomain)
                .toList();
    }
}
