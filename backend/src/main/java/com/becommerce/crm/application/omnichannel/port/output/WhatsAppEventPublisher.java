package com.becommerce.crm.application.omnichannel.port.output;

import com.becommerce.crm.application.omnichannel.event.WhatsAppAutoAiEvent;
import com.becommerce.crm.application.omnichannel.event.WhatsAppInboundEvent;
import com.becommerce.crm.application.omnichannel.event.WhatsAppSendEvent;

/**
 * Porta de publicação dos eventos do fluxo WhatsApp/UAZAPI (Sprint 23).
 * Implementada por infraestrutura RabbitMQ — o domínio/aplicação nunca
 * dependem de RabbitTemplate/@RabbitListener.
 *
 * <p>Publicar é <b>idempotente a nível de commit</b> para o caminho do webhook:
 * a implementação registra a publicação no AFTER_COMMIT da transação atual
 * (quando houver), evitando evento para dados não commitados.
 */
public interface WhatsAppEventPublisher {

    /** Mensagem entrante registrada pelo webhook -> fila de inbound. */
    void publishInbound(WhatsAppInboundEvent event);

    /** Decisão de IA para uma mensagem entrante -> fila de auto-ai. */
    void publishAutoAi(WhatsAppAutoAiEvent event);

    /** Envio de mensagem OUTBOUND -> fila de sender (provider). */
    void publishSend(WhatsAppSendEvent event);
}