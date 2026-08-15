package com.becommerce.crm.application.omnichannel.dto;

import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.domain.omnichannel.ConversationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/** Detalhe de uma conversa (cabeçalho + mensagens paginadas). */
public record ConversationDetailResponse(
        UUID id,
        UUID channelId,
        UUID contactId,
        String externalPhone,
        ConversationStatus status,
        LocalDateTime lastMessageAt,
        int unreadCount,
        PageResponse<MessageResponse> messages
) {
}