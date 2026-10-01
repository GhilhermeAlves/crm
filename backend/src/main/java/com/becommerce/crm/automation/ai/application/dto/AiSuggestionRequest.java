package com.becommerce.crm.automation.ai.application.dto;

/**
 * Requisição de sugestão de resposta para uma conversa omnichannel.
 */
public record AiSuggestionRequest(
        String conversationId
) {
}