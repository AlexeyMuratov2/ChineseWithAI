package ru.chinesewithai.backend.agentruntime.infrastructure.profile;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAgentProfileJpaRepository extends JpaRepository<AgentProfileJpaEntity, String> {
    List<AgentProfileJpaEntity> findAllByVisibleTrueOrderByDisplayNameAsc();
}
