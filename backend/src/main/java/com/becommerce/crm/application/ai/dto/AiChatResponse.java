package com.becommerce.crm.application.ai.dto;

import java.util.UUID;

/**
 * Resposta do chat do assistente de IA (AI-01).
 */
public record AiChatResponse(
        UUID conversationId,
        String message,
        String provider
) {
}