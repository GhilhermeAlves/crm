package com.becommerce.crm.infrastructure.rabbit;

import com.becommerce.crm.application.followup.event.FollowUpExecutionEvent;
import com.becommerce.crm.application.followup.port.output.FollowUpEventPublisher;
import com.becommerce.crm.application.omnichannel.event.WhatsAppAutoAiEvent;
import com.becommerce.crm.application.omnichannel.event.WhatsAppInboundEvent;
import com.becommerce.crm.application.omnichannel.event.WhatsAppSendEvent;
import com.becommerce.crm.application.omnichannel.port.output.WhatsAppEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Publicador RabbitMQ dos eventos do fluxo WhatsApp/UAZAPI (Sprint 23).
 *
 * <p><b>Consistência DB-commit/Rabbit-publish:</b> quando chamado dentro de uma
 * transação ativa (ex.: webhook @Transactional), o publish é registrado em
 * AFTER_COMMIT — a mensagem só sai quando os dados já estão visíveis.
 * Fora de transação (scheduler/consumers) publica imediatamente.
 *
 * <p><b>Risco documentado (§27):</b> não há Outbox persistido — se o publish
 * falhar após o commit, o evento é perdido. Mitigações: (a) paths não-webhook
 * são recuperáveis por natureza (reclaim de follow-up; reserva idempotente de
 * auto-resposta); (b) health do broker exposto no actuator; (c) logs estruturados
 * com {@code eventId} para diagnóstico. Implementação de Outbox completa fica
 * como débito para etapa posterior.
 */
@Component
public class RabbitMessagingEventPublisher implements WhatsAppEventPublisher, FollowUpEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitMessagingEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitMessagingEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishInbound(WhatsAppInboundEvent event) {
        publish(RabbitTopics.ROUTING_INBOUND, event.eventId(), event.companyId(), event);
    }

    @Override
    public void publishAutoAi(WhatsAppAutoAiEvent event) {
        publish(RabbitTopics.ROUTING_AUTO_AI, event.eventId(), event.companyId(), event);
    }

    @Override
    public void publishSend(WhatsAppSendEvent event) {
        publish(RabbitTopics.ROUTING_SENDER, event.eventId(), event.companyId(), event);
    }

    @Override
    public void publishExecution(FollowUpExecutionEvent event) {
        publish(RabbitTopics.ROUTING_FOLLOWUP_EXECUTOR, event.eventId(), event.companyId(), event);
    }

    private void publish(String routingKey, java.util.UUID eventId, java.util.UUID companyId, Object payload) {
        log.info("[WHATSAPP][PUBLISH] routingKey={} eventId={} companyId={}",
                routingKey, eventId, companyId);
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doPublish(routingKey, eventId, companyId, payload);
                }
            });
        } else {
            doPublish(routingKey, eventId, companyId, payload);
        }
    }

    private void doPublish(String routingKey, java.util.UUID eventId, java.util.UUID companyId, Object payload) {
        rabbitTemplate.convertAndSend(RabbitTopics.WHATSAPP_EXCHANGE, routingKey, payload);
        log.info("[WHATSAPP][PUBLISHED] routingKey={} eventId={} companyId={}",
                routingKey, eventId, companyId);
    }
}