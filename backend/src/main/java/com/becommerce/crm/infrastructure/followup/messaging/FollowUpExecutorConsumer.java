package com.becommerce.crm.infrastructure.followup.messaging;

import com.becommerce.crm.application.followup.event.FollowUpExecutionEvent;
import com.becommerce.crm.application.followup.service.FollowUpExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Infraestrutura: consumer da fila {@code crm.followup.executor} (Sprint 23).
 * Camada fina — delega para o {@link FollowUpExecutionService} (application
 * layer), que revalida Human Takeover/staleness e prepara o envio assíncrono.
 */
@Component
@ConditionalOnProperty(name = "crm.messaging.consumers.enabled", havingValue = "true", matchIfMissing = true)
public class FollowUpExecutorConsumer {

    private static final Logger log = LoggerFactory.getLogger(FollowUpExecutorConsumer.class);

    private final FollowUpExecutionService executionService;

    public FollowUpExecutorConsumer(FollowUpExecutionService executionService) {
        this.executionService = executionService;
    }

    @RabbitListener(queues = "crm.followup.executor")
    public void onExecution(FollowUpExecutionEvent event) {
        log.info("[FOLLOWUP][EXECUTOR] consome evento {}", event.eventId());
        executionService.execute(event);
    }
}