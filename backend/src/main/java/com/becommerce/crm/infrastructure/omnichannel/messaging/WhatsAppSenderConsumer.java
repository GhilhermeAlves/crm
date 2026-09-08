package com.becommerce.crm.infrastructure.omnichannel.messaging;

import com.becommerce.crm.application.omnichannel.event.WhatsAppSendEvent;
import com.becommerce.crm.application.omnichannel.service.WhatsAppSendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Infraestrutura: consumer da fila {@code crm.whatsapp.sender} (Sprint 23).
 * Camada fina — delega para o {@link WhatsAppSendService} (application layer).
 * Este é o ÚNICO ponto onde o provider (UAZAPI/MessageMedia atrás de
 * {@code WhatsAppProvider}) é chamado, fora do request HTTP.
 */
@Component
@ConditionalOnProperty(name = "crm.messaging.consumers.enabled", havingValue = "true", matchIfMissing = true)
public class WhatsAppSenderConsumer {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppSenderConsumer.class);

    private final WhatsAppSendService sendService;

    public WhatsAppSenderConsumer(WhatsAppSendService sendService) {
        this.sendService = sendService;
    }

    @RabbitListener(queues = "crm.whatsapp.sender")
    public void onSend(WhatsAppSendEvent event) {
        log.info("[WHATSAPP][SENDER] consome evento {}", event.eventId());
        sendService.send(event);
    }
}