package com.becommerce.crm.application.ai.dto;

import java.util.UUID;

/**
 * Resposta da sugestão de IA: o texto sugerido (opcional) e o provider usado.
 */
public record AiSuggestionResponse(
        UUID conversationId,
        String suggestion,
        String provider
) {
}