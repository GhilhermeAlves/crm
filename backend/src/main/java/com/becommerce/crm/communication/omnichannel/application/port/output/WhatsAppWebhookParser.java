package com.becommerce.crm.communication.omnichannel.application.port.output;

import com.becommerce.crm.communication.omnichannel.domain.MessageStatus;

import java.util.Map;
import java.util.Optional;

/**
 * Normaliza payloads de webhook de provider para eventos normalizados,
 * desacoplando o serviço da estrutura específica do provider.
 */
public interface WhatsAppWebhookParser {

    /** Mensagem recebida normalizada. */
    record InboundMessageData(String externalMessageId, String from, String to, String body,
                              String senderName) {
        public InboundMessageData(String externalMessageId, String from, String to, String body) {
            this(externalMessageId, from, to, body, null);
        }
    }

    /** Atualização de status (SENT/DELIVERED/READ/FAILED) normalizada. */
    record StatusData(String externalMessageId, MessageStatus status, String error) {
    }

    boolean isInboundMessage(Map<String, Object> raw);

    Optional<InboundMessageData> parseInboundMessage(Map<String, Object> raw);

    boolean isStatusUpdate(Map<String, Object> raw);

    Optional<StatusData> parseStatusUpdate(Map<String, Object> raw);

    /** Referência de canal (instância do provider = externalId do canal), p/ resolver a empresa. */
    String providerChannelReference(Map<String, Object> raw);

    /** Nome do provider (para logs, sem expor secrets). */
    String providerName();
}