package ru.chinesewithai.backend.agentruntime.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAgentStepJpaRepository extends JpaRepository<AgentStepJpaEntity, UUID> {
    List<AgentStepJpaEntity> findBySessionIdOrderByStepIndexAsc(UUID sessionId);
}
