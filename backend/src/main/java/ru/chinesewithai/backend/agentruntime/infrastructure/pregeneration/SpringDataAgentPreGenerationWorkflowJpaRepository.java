package ru.chinesewithai.backend.agentruntime.infrastructure.pregeneration;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAgentPreGenerationWorkflowJpaRepository
        extends JpaRepository<AgentPreGenerationWorkflowJpaEntity, Long> {

    Optional<AgentPreGenerationWorkflowJpaEntity> findByProfileKeyAndWorkflowVariantKeyAndActiveTrue(
            String profileKey, String workflowVariantKey);

    Optional<AgentPreGenerationWorkflowJpaEntity> findByProfileKeyAndWorkflowVariantKeyIsNullAndActiveTrue(
            String profileKey);
}
