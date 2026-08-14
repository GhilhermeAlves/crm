package com.becommerce.crm.infrastructure.workflow.listener;

import com.becommerce.crm.application.workflow.service.WorkflowExecutor;
import com.becommerce.crm.domain.workflow.event.WorkflowTriggerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Consome os {@link WorkflowTriggerEvent} publicados pelo {@code EventPublisher}
 * da aplicação (reaproveita o mecanismo de eventos existente — Item 12). Execução
 * síncrona e determinística; falhas em ações são isoladas e registradas pelo
 * {@link WorkflowExecutor}/{@code WorkflowActionRunner}.
 */
@Component
public class WorkflowEventListener {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEventListener.class);

    private final WorkflowExecutor workflowExecutor;

    public WorkflowEventListener(WorkflowExecutor workflowExecutor) {
        this.workflowExecutor = workflowExecutor;
    }

    @EventListener
    public void handle(WorkflowTriggerEvent event) {
        try {
            workflowExecutor.process(event);
        } catch (Exception e) {
            log.error("Falha no processamento de workflow: {}", e.getMessage(), e);
        }
    }
}