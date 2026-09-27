package com.becommerce.crm.infrastructure.ai.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AgentActionAuditJpaRepository extends JpaRepository<AgentActionAuditJpaEntity, UUID> {

    Page<AgentActionAuditJpaEntity> findByConversationIdOrderByExecutedAtDesc(UUID conversationId,
                                                                              Pageable pageable);

    Page<AgentActionAuditJpaEntity> findByCompanyIdOrderByExecutedAtDesc(UUID companyId, Pageable pageable);
}
