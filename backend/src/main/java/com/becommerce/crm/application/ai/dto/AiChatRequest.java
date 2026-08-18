package com.becommerce.crm.application.ai.dto;

import java.util.UUID;

/**
 * Requisição do chat do assistente de IA (AI-01). {@code conversationId} é
 * opcional — quando ausente, uma nova conversa é criada com o contexto atual.
 */
public record AiChatRequest(
        String message,
        UUID conversationId,
        AiContextPayload context
) {
}
