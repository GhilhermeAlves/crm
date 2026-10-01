package com.becommerce.crm.automation.workflow.application.port.output;

import com.becommerce.crm.automation.workflow.domain.ActionType;
import com.becommerce.crm.automation.workflow.domain.ExecutionStatus;
import com.becommerce.crm.automation.workflow.domain.WorkflowExecution;

import java.util.List;
import java.util.UUID;

public interface WorkflowExecutionRepository {

    /**
     * Insere a execução de forma idempotente (chave company/action/event).
     * Retorna 1 se inseriu, 0 se já existia (deve ser ignorado — SKIP).
     */
    int insertNew(UUID id, UUID companyId, UUID workflowId, UUID workflowActionId, UUID eventId,
                  String eventType, UUID entityId, ActionType actionType);

    void updateResult(UUID id, UUID companyId, ExecutionStatus status, String resultText, String errorMessage);

    List<WorkflowExecution> findByCompanyIdAndWorkflowId(UUID companyId, UUID workflowId);

    List<WorkflowExecution> findByCompanyId(UUID companyId);

    List<WorkflowExecution> findByCompanyIdAndWorkflowIdAndEventId(UUID companyId, UUID workflowId, UUID eventId);
}
