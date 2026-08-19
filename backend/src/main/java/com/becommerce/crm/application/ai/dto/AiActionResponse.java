package com.becommerce.crm.application.ai.dto;

import com.becommerce.crm.domain.ai.AiAction;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Representacao de uma acao de escrita proposta pelo assistente de IA (AI-05).
 * Exposta ao frontend para renderizar o cartao de confirmacao com a descricao,
 * o estado e os parametros tipados. Nunca expoe companyId/userId - a
 * propriedade e sempre derivada do usuario autenticado no backend.
 */
public record AiActionResponse(
        UUID id,
        UUID conversationId,
        String tool,
        String entityType,
        UUID entityId,
        String description,
        String status,
        Map<String, Object> parameters,
        Object result,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static AiActionResponse from(AiAction action) {
        return new AiActionResponse(
                action.getId(),
                action.getConversationId(),
                action.getTool(),
                action.getEntityType(),
                action.getEntityId(),
                action.getDescription(),
                action.getStatus() != null ? action.getStatus().name() : null,
                action.getParameters(),
                action.getResult(),
                action.getErrorMessage(),
                action.getCreatedAt(),
                action.getUpdatedAt());
    }
}