package com.becommerce.crm.automation.ai.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AgentConfigJpaRepository extends JpaRepository<AgentConfigJpaEntity, UUID> {

    Optional<AgentConfigJpaEntity> findByCompanyId(UUID companyId);
}