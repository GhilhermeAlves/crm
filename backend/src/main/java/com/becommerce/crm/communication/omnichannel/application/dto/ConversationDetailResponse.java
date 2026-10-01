package com.becommerce.crm.communication.omnichannel.application.dto;

import com.becommerce.crm.shared.application.dto.PageResponse;
import com.becommerce.crm.communication.omnichannel.domain.ConversationMode;
import com.becommerce.crm.communication.omnichannel.domain.ConversationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/** Detalhe de uma conversa (cabeçalho + mensagens paginadas). */
public record ConversationDetailResponse(
        UUID id,
        UUID channelId,
        UUID contactId,
        String externalPhone,
        ConversationStatus status,
        ConversationMode mode,
        LocalDateTime lastMessageAt,
        int unreadCount,
        PageResponse<MessageResponse> messages
) {
}