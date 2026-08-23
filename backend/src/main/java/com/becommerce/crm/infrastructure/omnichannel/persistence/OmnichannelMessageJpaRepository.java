package com.becommerce.crm.infrastructure.omnichannel.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface OmnichannelMessageJpaRepository extends JpaRepository<OmnichannelMessageJpaEntity, UUID> {

    Optional<OmnichannelMessageJpaEntity> findByExternalMessageId(String externalMessageId);

    Optional<OmnichannelMessageJpaEntity> findByClientMessageId(UUID clientMessageId);

    Page<OmnichannelMessageJpaEntity> findByConversationIdOrderByCreatedAtAsc(UUID conversationId, Pageable pageable);

    Optional<OmnichannelMessageJpaEntity> findFirstByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    /** Insert idempotente por (company_id, external_message_id) — ON CONFLICT DO NOTHING. */
    @Modifying
    @Query(value = """
            INSERT INTO omnichannel_messages
                (id, company_id, conversation_id, channel_id, direction, sender_phone,
                 recipient_phone, type, body, status, external_message_id, client_message_id,
                 provider_error, sent_at, received_at, created_at, updated_at)
            VALUES (:id, :companyId, :conversationId, :channelId, :direction, :senderPhone,
                    :recipientPhone, :type, :body, :status, :externalMessageId, :clientMessageId,
                    :providerError, :sentAt, :receivedAt, :createdAt, :updatedAt)
            ON CONFLICT (company_id, external_message_id) DO NOTHING
            """, nativeQuery = true)
    int insertInboundIdempotent(
            @Param("id") UUID id,
            @Param("companyId") UUID companyId,
            @Param("conversationId") UUID conversationId,
            @Param("channelId") UUID channelId,
            @Param("direction") String direction,
            @Param("senderPhone") String senderPhone,
            @Param("recipientPhone") String recipientPhone,
            @Param("type") String type,
            @Param("body") String body,
            @Param("status") String status,
            @Param("externalMessageId") String externalMessageId,
            @Param("clientMessageId") UUID clientMessageId,
            @Param("providerError") String providerError,
            @Param("sentAt") LocalDateTime sentAt,
            @Param("receivedAt") LocalDateTime receivedAt,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Query(value = """
            UPDATE omnichannel_messages
            SET status = :status, provider_error = :providerError, updated_at = :updatedAt
            WHERE external_message_id = :externalMessageId AND company_id = :companyId
            """, nativeQuery = true)
    int updateStatusByExternalId(
            @Param("companyId") UUID companyId,
            @Param("externalMessageId") String externalMessageId,
            @Param("status") String status,
            @Param("providerError") String providerError,
            @Param("updatedAt") LocalDateTime updatedAt);
}
