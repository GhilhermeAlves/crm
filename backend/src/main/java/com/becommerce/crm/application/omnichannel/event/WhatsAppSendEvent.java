package com.becommerce.crm.application.omnichannel.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento de envio via provider (Sprint 23). Publicado pelo processor de IA ou
 * pelo executor de follow-up e consumido pela fila {@code crm.whatsapp.sender}.
 *
 * <p>Transporta apenas o necessário: a mensagem OUTBOUND já foi persistida como
 * PENDING pelo produtor (idempotência por estado — o consumer só envia se ainda
 * estiver PENDING). {@code followUpId} é opcional e, quando presente, o consumer
 * também atualiza o follow-up associado (SENT / retry / terminal).
 */
public record WhatsAppSendEvent(
        UUID eventId,
        UUID companyId,
        UUID conversationId,
        UUID outboundMessageId,
        UUID channelId,
        String to,
        String body,
        UUID followUpId,
        LocalDateTime occurredAt
) {

    public static WhatsAppSendEvent of(UUID companyId, UUID conversationId, UUID outboundMessageId,
                                       UUID channelId, String to, String body) {
        return new WhatsAppSendEvent(UUID.randomUUID(), companyId, conversationId, outboundMessageId,
                channelId, to, body, null, LocalDateTime.now());
    }

    public static WhatsAppSendEvent ofFollowUp(UUID companyId, UUID conversationId,
                                               UUID outboundMessageId, UUID channelId,
                                               String to, String body, UUID followUpId) {
        return new WhatsAppSendEvent(UUID.randomUUID(), companyId, conversationId, outboundMessageId,
                channelId, to, body, followUpId, LocalDateTime.now());
    }
}