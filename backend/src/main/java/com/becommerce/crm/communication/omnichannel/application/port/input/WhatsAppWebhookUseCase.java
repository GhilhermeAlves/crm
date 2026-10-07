package com.becommerce.crm.communication.omnichannel.application.port.input;

import java.util.Map;

/**
 * Webhook de WhatsApp (recebimento de mensagens e atualizações de status).
 * Deve ser idempotente: o mesmo evento externo repetido produz um único
 * registro (FASE 6/10/17).
 */
public interface WhatsAppWebhookUseCase {

    /** Processa um evento de entrada (mensagem ou status) de forma idempotente. */
    void handleEvent(Map<String, Object> payload);
}