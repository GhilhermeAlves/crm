package com.becommerce.crm.automation.ai.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentToolJpaRepository extends JpaRepository<AgentToolJpaEntity, UUID> {

    List<AgentToolJpaEntity> findByCompanyIdOrderByToolNameAsc(UUID companyId);

    Optional<AgentToolJpaEntity> findByCompanyIdAndToolName(UUID companyId, String toolName);
}
