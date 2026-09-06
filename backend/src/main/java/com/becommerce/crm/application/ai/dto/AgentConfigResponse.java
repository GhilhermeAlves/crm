package com.becommerce.crm.application.ai.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Configuração do agente de IA no formato de resposta (Sprint 3).
 *
 * <p>NUNCA expõe segredos/API keys — apenas os parâmetros operacionais usados
 * pelo {@code WhatsAppInboundAutoReplyProcessor}. Quando o registro ainda não
 * existe (safe default), {@code id} e {@code updatedAt} são {@code null} e os
 * parâmetros retornam os defaults de fábrica ({@code aiEnabled=false},
 * {@code allowAutoReply=false}, {@code cooldownMinutes=60}, {@code maxChars=1000}).
 */
public record AgentConfigResponse(
        UUID id,
        boolean aiEnabled,
        boolean allowAutoReply,
        String systemPrompt,
        String model,
        Double temperature,
        Integer maxTokens,
        int cooldownMinutes,
        int maxChars,
        LocalDateTime updatedAt
) {
}