package com.becommerce.crm.automation.ai.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiMessageJpaRepository extends JpaRepository<AiMessageJpaEntity, UUID> {

    List<AiMessageJpaEntity> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);
}
