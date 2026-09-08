package com.becommerce.crm.application.omnichannel.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento de decisão automática de IA (Sprint 23). Publicado pelo processor de
 * inbound quando a conversa está em modo AUTOMATIC e consumido pela fila
 * {@code crm.whatsapp.auto-ai}.
 *
 * <p>O consumer REVALIDA o estado atual da conversa no momento da execução
 * (Human Takeover pode ter ocorrido entre a publicação e o consumo), além de
 * verificar AgentConfig, cooldown e reserva idempotente por mensagem entrante.
 */
public record WhatsAppAutoAiEvent(
        UUID eventId,
        UUID companyId,
        UUID conversationId,
        UUID inboundMessageId,
        String from,
        String body,
        LocalDateTime occurredAt
) {

    public static WhatsAppAutoAiEvent of(UUID companyId, UUID conversationId, UUID inboundMessageId,
                                         String from, String body) {
        return new WhatsAppAutoAiEvent(UUID.randomUUID(), companyId, conversationId,
                inboundMessageId, from, body, LocalDateTime.now());
    }
}