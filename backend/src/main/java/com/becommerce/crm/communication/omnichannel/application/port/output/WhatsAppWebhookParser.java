package com.becommerce.crm.communication.omnichannel.application.port.output;

import com.becommerce.crm.communication.omnichannel.domain.MessageStatus;
import com.becommerce.crm.communication.omnichannel.domain.MessageType;

import java.util.Map;
import java.util.Optional;

/**
 * Normaliza payloads de webhook de provider para eventos normalizados,
 * desacoplando o serviço da estrutura específica do provider.
 */
public interface WhatsAppWebhookParser {

    /** Mensagem recebida normalizada. */
    /**
     * Mensagem normalizada. {@code from} é sempre o telefone do contato externo;
     * {@code fromMe} indica que ela saiu do próprio número do canal (enviada
     * pelo CRM ou digitada no celular do dono do número).
     */
    record InboundMessageData(String externalMessageId, String from, String to, String body,
                              String senderName, boolean fromMe, MessageType type) {
        public InboundMessageData(String externalMessageId, String from, String to, String body) {
            this(externalMessageId, from, to, body, null, false, MessageType.TEXT);
        }

        public InboundMessageData(String externalMessageId, String from, String to, String body,
                                  String senderName) {
            this(externalMessageId, from, to, body, senderName, false, MessageType.TEXT);
        }

        public InboundMessageData(String externalMessageId, String from, String to, String body,
                                  String senderName, boolean fromMe) {
            this(externalMessageId, from, to, body, senderName, fromMe, MessageType.TEXT);
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