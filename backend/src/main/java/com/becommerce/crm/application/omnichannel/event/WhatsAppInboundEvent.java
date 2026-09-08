package com.becommerce.crm.application.omnichannel.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento interno de mensagem entrante publicada após o webhook (Sprint 23).
 *
 * <p>Payload mínimo e tipado para o RabbitMQ (sem objetos JPA). O webhook
 * persiste Conversation/Message de forma idempotente e publica este evento no
 * commit; o consumer {@code crm.whatsapp.inbound} decide o próximo passo
 * (IA automática / pular em modo humano).
 */
public record WhatsAppInboundEvent(
        UUID eventId,
        UUID companyId,
        UUID conversationId,
        UUID messageId,
        UUID channelId,
        String externalMessageId,
        String from,
        String body,
        LocalDateTime occurredAt
) {

    public static WhatsAppInboundEvent of(UUID companyId, UUID conversationId, UUID messageId,
                                          UUID channelId, String externalMessageId,
                                          String from, String body) {
        return new WhatsAppInboundEvent(UUID.randomUUID(), companyId, conversationId, messageId,
                channelId, externalMessageId, from, body, LocalDateTime.now());
    }
}