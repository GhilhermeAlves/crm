package com.becommerce.crm.application.workflow.service;

import com.becommerce.crm.application.workflow.port.output.WorkflowRepository;
import com.becommerce.crm.application.workflow.port.output.WorkflowRunRepository;
import com.becommerce.crm.application.workflow.dto.ConditionEvaluation;
import com.becommerce.crm.domain.workflow.Workflow;
import com.becommerce.crm.domain.workflow.WorkflowAction;
import com.becommerce.crm.domain.workflow.WorkflowRunStatus;
import com.becommerce.crm.domain.workflow.event.WorkflowTriggerEvent;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
    private final WorkflowRunRepository runRepository;
    private final ObjectMapper objectMapper;

    public WorkflowExecutor(WorkflowRepository workflowRepository,
                            WorkflowConditionEvaluator conditionEvaluator,
                            WorkflowActionRunner actionRunner,
                            WorkflowRunRepository runRepository,
                            ObjectMapper objectMapper) {
        this.workflowRepository = workflowRepository;
        this.conditionEvaluator = conditionEvaluator;
        this.actionRunner = actionRunner;
        this.runRepository = runRepository;
        this.objectMapper = objectMapper;
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
                List<ConditionEvaluation> conditions = conditionEvaluator.evaluate(workflow, event);
                boolean matched = conditions.stream().allMatch(ConditionEvaluation::matched);

                UUID runId = UUID.randomUUID();
                int inserted = runRepository.insertNew(runId, companyId, workflow.getId(), event.eventId(),
                        event.trigger().name(), entityId(event), matched ? WorkflowRunStatus.MATCHED : WorkflowRunStatus.SKIPPED,
                        toJson(conditions), toJson(event.context()));
                if (inserted == 0) {
                    continue; // run já registrado para (company, workflow, event)
                }
                if (!matched) {
                    runRepository.updateStatus(runId, companyId, WorkflowRunStatus.SKIPPED, null);
                    continue;
                }

                List<WorkflowAction> actions = workflow.getActions().stream()
                        .sorted(Comparator.comparingInt(WorkflowAction::getSortOrder))
                        .toList();
                int failures = 0;
                int executed = 0;
                for (WorkflowAction action : actions) {
                    executed++;
                    try {
                        actionRunner.run(workflow, action, event);
                    } catch (Exception ex) {
                        failures++;
                        try {
                            actionRunner.recordFailure(workflow, action, event, ex);
                        } catch (Exception recordEx) {
                            log.error("Não foi possível registrar falha de workflow "
                                    + "workflow={} action={}: {}", workflow.getName(),
                                    action.getActionType(), recordEx.getMessage(), recordEx);
                        }
                    }
                }
                WorkflowRunStatus finalStatus = failures == 0
                        ? WorkflowRunStatus.SUCCESS
                        : (failures == executed ? WorkflowRunStatus.FAILED : WorkflowRunStatus.PARTIAL);
                runRepository.updateStatus(runId, companyId, finalStatus, null);
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

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private static UUID entityId(WorkflowTriggerEvent event) {
        if (event.opportunityId() != null) {
            return event.opportunityId();
        }
        if (event.taskId() != null) {
            return event.taskId();
        }
        if (event.activityId() != null) {
            return event.activityId();
        }
        return event.contactId();
    }
}