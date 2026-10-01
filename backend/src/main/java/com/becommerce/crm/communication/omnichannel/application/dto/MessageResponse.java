package com.becommerce.crm.communication.omnichannel.application.dto;

import com.becommerce.crm.communication.omnichannel.domain.MessageDirection;
import com.becommerce.crm.communication.omnichannel.domain.MessageStatus;
import com.becommerce.crm.communication.omnichannel.domain.MessageType;

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