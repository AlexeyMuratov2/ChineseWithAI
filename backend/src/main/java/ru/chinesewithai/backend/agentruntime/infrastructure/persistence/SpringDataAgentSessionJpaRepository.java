package ru.chinesewithai.backend.agentruntime.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAgentSessionJpaRepository extends JpaRepository<AgentSessionJpaEntity, UUID> {}
