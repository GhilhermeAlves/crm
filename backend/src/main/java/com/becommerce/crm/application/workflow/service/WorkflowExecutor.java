package com.becommerce.crm.application.workflow.service;

import com.becommerce.crm.application.workflow.port.output.WorkflowRepository;
import com.becommerce.crm.domain.workflow.Workflow;
import com.becommerce.crm.domain.workflow.WorkflowAction;
import com.becommerce.crm.domain.workflow.event.WorkflowTriggerEvent;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Executor de workflows (Item 5). Recebe um {@link WorkflowTriggerEvent}, busca
 * os workflows ATIVOS da empresa para o trigger, avalia as condições e, para
 * cada condição atendida, executa as ações em ordem.
 *
 * <p>Segurança (Item 14): apenas tipos de ação controlados pelo backend;
 * nenhum código/expressão arbitrária é executado. Idempotência (Item 6) e
 * isolamento por empresa são garantidos na persistência (RLS + chave única).
 *
 * <p>Guarda de recursão: enquanto processa, novos eventos gerados pelas próprias
 * ações (Task/Activity criadas) são ignorados preventivamente para evitar loops
 * (Task → workflow → Task → ...).
 */
@Service
public class WorkflowExecutor {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutor.class);
    private static final ThreadLocal<Boolean> PROCESSING = new ThreadLocal<>();

    private final WorkflowRepository workflowRepository;
    private final WorkflowConditionEvaluator conditionEvaluator;
    private final WorkflowActionRunner actionRunner;

    public WorkflowExecutor(WorkflowRepository workflowRepository,
                            WorkflowConditionEvaluator conditionEvaluator,
                            WorkflowActionRunner actionRunner) {
        this.workflowRepository = workflowRepository;
        this.conditionEvaluator = conditionEvaluator;
        this.actionRunner = actionRunner;
    }

    public void process(WorkflowTriggerEvent event) {
        if (Boolean.TRUE.equals(PROCESSING.get())) {
            return;
        }
        UUID companyId = event.companyId();
        if (companyId == null) {
            return;
        }

        PROCESSING.set(true);
        try {
            TenantContext.setCompanyId(companyId);
            List<Workflow> active = workflowRepository
                    .findByCompanyIdAndTriggerAndActive(companyId, event.trigger(), true);

            for (Workflow workflow : active) {
                if (!conditionEvaluator.matches(workflow, event)) {
                    continue;
                }
                List<WorkflowAction> actions = workflow.getActions().stream()
                        .sorted(Comparator.comparingInt(WorkflowAction::getSortOrder))
                        .toList();
                for (WorkflowAction action : actions) {
                    try {
                        actionRunner.run(workflow, action, event);
                    } catch (Exception ex) {
                        try {
                            actionRunner.recordFailure(workflow, action, event, ex);
                        } catch (Exception recordEx) {
                            log.error("Não foi possível registrar falha de workflow "
                                    + "workflow={} action={}: {}", workflow.getName(),
                                    action.getActionType(), recordEx.getMessage(), recordEx);
                        }
                    }
                }
            }
        } finally {
            PROCESSING.remove();
            // NOT TenantContext.clear(): the publisher service owns tenant lifecycle;
            // this listener runs synchronously within its try/finally.
        }
    }

    public boolean isProcessing() {
        return Boolean.TRUE.equals(PROCESSING.get());
    }
}