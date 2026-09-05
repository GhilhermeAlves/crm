package com.becommerce.crm.infrastructure.ai.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface AgentAutoReplyJpaRepository extends JpaRepository<AgentAutoReplyJpaEntity, UUID> {

    Optional<AgentAutoReplyJpaEntity> findFirstByCompanyIdAndConversationIdOrderByRepliedAtDesc(
            UUID companyId, UUID conversationId);

    /**
     * Reserva idempotente de auto-resposta por (company_id, inbound_message_id) —
     * ON CONFLICT DO NOTHING. Retorna o número de linhas inseridas (0 = já reservada).
     */
    @Modifying
    @Query(value = """
            INSERT INTO agent_auto_replies (id, company_id, conversation_id, inbound_message_id, replied_at)
            VALUES (:id, :companyId, :conversationId, :inboundMessageId, :repliedAt)
            ON CONFLICT (company_id, inbound_message_id) DO NOTHING
            """, nativeQuery = true)
    int reserve(
            @Param("id") UUID id,
            @Param("companyId") UUID companyId,
            @Param("conversationId") UUID conversationId,
            @Param("inboundMessageId") UUID inboundMessageId,
            @Param("repliedAt") LocalDateTime repliedAt);
}