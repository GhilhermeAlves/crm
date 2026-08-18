package com.becommerce.crm.infrastructure.ai.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiConversationJpaRepository extends JpaRepository<AiConversationJpaEntity, UUID> {

    List<AiConversationJpaEntity> findByCompanyIdAndUserIdOrderByUpdatedAtDesc(UUID companyId, UUID userId);
}
