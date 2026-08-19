package com.becommerce.crm.application.ai.dto;

import com.becommerce.crm.domain.ai.AiConversation;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resumo de uma conversa do assistente de IA (AI-04). Usado na listagem do
 * histórico do usuário. Nunca expõe companyId/userId - a propriedade é sempre
 * derivada do usuário autenticado no backend.
 */
public record AiConversationResponse(
        UUID id,
        String title,
        String screen,
        UUID recordId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static AiConversationResponse from(AiConversation conversation) {
        return new AiConversationResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getScreen(),
                conversation.getRecordId(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt());
    }
}