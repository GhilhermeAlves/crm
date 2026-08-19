package com.becommerce.crm.application.ai.dto;

import java.util.List;
import java.util.UUID;

/**
 * Resposta do chat do assistente de IA (AI-01).
 *
 * <p>AI-05: {@code actions} lista as propostas de escrita criadas durante esta
 * chamada (vazio para mensagens normais). Campo aditivo - o contrato anterior
 * (conversationId/message/provider) permanece inalterado.</p>
 */
public record AiChatResponse(
        UUID conversationId,
        String message,
        String provider,
        List<AiActionResponse> actions
) {

    public AiChatResponse(UUID conversationId, String message, String provider) {
        this(conversationId, message, provider, List.of());
    }
}