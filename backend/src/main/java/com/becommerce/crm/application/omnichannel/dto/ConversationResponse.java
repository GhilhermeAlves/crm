package com.becommerce.crm.application.omnichannel.dto;

import com.becommerce.crm.domain.omnichannel.ConversationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/** Conversa no formato de resposta (lista Inbox). */
public record ConversationResponse(
        UUID id,
        UUID channelId,
        UUID contactId,
        String externalPhone,
        ConversationStatus status,
        LocalDateTime lastMessageAt,
        String lastMessage,
        int unreadCount,
        LocalDateTime createdAt
) {
}