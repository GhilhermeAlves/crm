package com.becommerce.crm.application.omnichannel.port.output;

import com.becommerce.crm.domain.omnichannel.MessageStatus;

import java.util.Map;
import java.util.Optional;

/**
 * Normaliza payloads de webhook de provider para eventos normalizados,
 * desacoplando o serviço da estrutura específica da Meta.
 */
public interface WhatsAppWebhookParser {

    /** Mensagem recebida normalizada. */
    record InboundMessageData(String externalMessageId, String from, String to, String body) {
    }

    /** Atualização de status (SENT/DELIVERED/READ/FAILED) normalizada. */
    record StatusData(String externalMessageId, MessageStatus status, String error) {
    }

    /** Identificação de webhook (para verificação de assinatura GET). */
    record Verification(String mode, String token, String challenge) {
    }

    boolean isInboundMessage(Map<String, Object> raw);

    Optional<InboundMessageData> parseInboundMessage(Map<String, Object> raw);

    boolean isStatusUpdate(Map<String, Object> raw);

    Optional<StatusData> parseStatusUpdate(Map<String, Object> raw);

    Verification parseVerification(Map<String, String> params);

    /** Referência de canal (número/phone_number_id da empresa) elemento do evento, p/ resolver a empresa. */
    String providerChannelReference(Map<String, Object> raw);

    /** Nome do provider (para logs, sem expor secrets). */
    String providerName();
}