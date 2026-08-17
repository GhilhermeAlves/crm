package com.becommerce.crm.application.ai.dto;

/**
 * Requisição de sugestão de resposta para uma conversa omnichannel.
 */
public record AiSuggestionRequest(
        String conversationId
) {
}