package com.becommerce.crm.application.ai.dto;

import com.becommerce.crm.domain.ai.AiMessage;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mensagem de uma conversa do assistente de IA (AI-04). {@code role} indica o
 * emissor (user/assistant) e {@code content} o texto trocado.
 */
public record AiMessageResponse(
        UUID id,
        UUID conversationId,
        String role,
        String content,
        LocalDateTime createdAt
) {

    public static AiMessageResponse from(AiMessage message) {
        return new AiMessageResponse(
                message.getId(),
                message.getConversationId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt());
    }
}