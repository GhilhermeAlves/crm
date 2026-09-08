package com.becommerce.crm.infrastructure.omnichannel.messaging;

import com.becommerce.crm.application.omnichannel.event.WhatsAppInboundEvent;
import com.becommerce.crm.application.omnichannel.service.WhatsAppInboundProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Infraestrutura: consumer da fila {@code crm.whatsapp.inbound} (Sprint 23).
 * Camada fina — delega 100% do roteamento lógico para o
 * {@link WhatsAppInboundProcessor} (application layer). Nenhuma regra aqui.
 */
@Component
@ConditionalOnProperty(name = "crm.messaging.consumers.enabled", havingValue = "true", matchIfMissing = true)
public class WhatsAppInboundConsumer {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppInboundConsumer.class);

    private final WhatsAppInboundProcessor processor;

    public WhatsAppInboundConsumer(WhatsAppInboundProcessor processor) {
        this.processor = processor;
    }

    @RabbitListener(queues = "crm.whatsapp.inbound")
    public void onInbound(WhatsAppInboundEvent event) {
        log.info("[WHATSAPP][INBOUND] consome evento {}", event.eventId());
        processor.execute(event);
    }
}