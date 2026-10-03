package com.becommerce.crm.communication.omnichannel.application.dto;

import com.becommerce.crm.communication.omnichannel.domain.ConversationMode;
import com.becommerce.crm.communication.omnichannel.domain.ConversationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/** Conversa no formato de resposta (lista Inbox). */
public record ConversationResponse(
        UUID id,
        UUID channelId,
        UUID contactId,
        String externalPhone,
        ConversationStatus status,
        ConversationMode mode,
        LocalDateTime lastMessageAt,
        String lastMessage,
        int unreadCount,
        LocalDateTime createdAt
) {
}