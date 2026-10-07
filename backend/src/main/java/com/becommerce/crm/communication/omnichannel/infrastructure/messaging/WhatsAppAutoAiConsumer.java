package com.becommerce.crm.communication.omnichannel.infrastructure.messaging;

import com.becommerce.crm.communication.omnichannel.application.event.WhatsAppAutoAiEvent;
import com.becommerce.crm.communication.omnichannel.application.service.WhatsAppInboundAutoReplyProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Infraestrutura: consumer da fila {@code crm.whatsapp.auto-ai} (Sprint 23).
 * Camada fina — delega para o {@link WhatsAppInboundAutoReplyProcessor}
 * (application layer), que revalida o estado e gera/publica a auto-resposta.
 * Executado fora do request HTTP do webhook.
 */
@Component
@ConditionalOnProperty(name = "crm.messaging.consumers.enabled", havingValue = "true", matchIfMissing = true)
public class WhatsAppAutoAiConsumer {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppAutoAiConsumer.class);

    private final WhatsAppInboundAutoReplyProcessor processor;

    public WhatsAppAutoAiConsumer(WhatsAppInboundAutoReplyProcessor processor) {
        this.processor = processor;
    }

    @RabbitListener(queues = "crm.whatsapp.auto-ai", concurrency = "${omnichannel.whatsapp.auto-ai-concurrency:2-8}")
    public void onAutoAi(WhatsAppAutoAiEvent event) {
        log.info("[WHATSAPP][AUTO-AI] consome evento {}", event.eventId());
        processor.processInbound(event.companyId(), event.conversationId(),
                event.inboundMessageId(), event.from(), event.body(), event.senderName());
    }
}