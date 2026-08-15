package com.becommerce.crm.application.omnichannel.dto;

import com.becommerce.crm.domain.omnichannel.MessageDirection;
import com.becommerce.crm.domain.omnichannel.MessageStatus;
import com.becommerce.crm.domain.omnichannel.MessageType;

import java.time.LocalDateTime;
import java.util.UUID;

/** Mensagem no formato de resposta. */
public record MessageResponse(
        UUID id,
        UUID conversationId,
        MessageDirection direction,
        String senderPhone,
        String recipientPhone,
        MessageType type,
        String body,
        MessageStatus status,
        String externalMessageId,
        String providerError,
        LocalDateTime sentAt,
        LocalDateTime createdAt
) {
}