package com.becommerce.crm.application.omnichannel.port.input;

import java.util.Map;

/**
 * Webhook de WhatsApp (recebimento de mensagens e atualizações de status).
 * Deve ser idempotente: o mesmo evento externo repetido produz um único
 * registro (FASE 6/10/17).
 */
public interface WhatsAppWebhookUseCase {

    /**
     * Verificação de webhook (GET com mode/token/challenge). Retorna o challenge
     * quando o token confere; {code null} caso contrário.
     */
    String verify(Map<String, String> params);

    /** Processa um evento de entrada (mensagem ou status) de forma idempotente. */
    void handleEvent(Map<String, Object> payload);
}