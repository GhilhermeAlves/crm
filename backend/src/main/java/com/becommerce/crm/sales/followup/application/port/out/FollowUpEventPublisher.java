package com.becommerce.crm.sales.followup.application.port.out;

import com.becommerce.crm.sales.followup.application.event.FollowUpExecutionEvent;

/**
 * Porta de publicação de eventos de follow-up (Sprint 23). Implementada por
 * infraestrutura RabbitMQ para desacoplar o scheduler do broker.
 */
public interface FollowUpEventPublisher {

    /** Follow-up já "claimado" atômicamente -> fila de executor. */
    void publishExecution(FollowUpExecutionEvent event);
}