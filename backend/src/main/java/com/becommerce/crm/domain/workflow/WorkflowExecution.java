package com.becommerce.crm.domain.workflow;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro de execução de um workflow (Item 7) e pilar da idempotência (Item 6).
 * A unicidade de {@code (companyId, workflowActionId, eventId)} na tabela impede
 * que o mesmo evento + mesma ação execute duas vezes (retry/restart/duplicata).
 */
public class WorkflowExecution {

    private final UUID id;
    private final UUID companyId;
    private final UUID workflowId;
    private final UUID workflowActionId;
    private final UUID eventId;
    private final String eventType;
    private final UUID entityId;
    private final ActionType actionType;
    private ExecutionStatus status;
    private String resultText;
    private String errorMessage;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private WorkflowExecution(UUID id, UUID companyId, UUID workflowId, UUID workflowActionId,
                              UUID eventId, String eventType, UUID entityId, ActionType actionType,
                              ExecutionStatus status, String resultText, String errorMessage,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.workflowId = workflowId;
        this.workflowActionId = workflowActionId;
        this.eventId = eventId;
        this.eventType = eventType;
        this.entityId = entityId;
        this.actionType = actionType;
        this.status = status;
        this.resultText = resultText;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static WorkflowExecution forAction(UUID id, UUID companyId, UUID workflowId, UUID workflowActionId,
                                              UUID eventId, String eventType, UUID entityId, ActionType actionType) {
        LocalDateTime now = LocalDateTime.now();
        return new WorkflowExecution(id, companyId, workflowId, workflowActionId, eventId, eventType,
                entityId, actionType, ExecutionStatus.PROCESSING, null, null, now, now);
    }

    public static WorkflowExecution reconstitute(UUID id, UUID companyId, UUID workflowId, UUID workflowActionId,
                                                 UUID eventId, String eventType, UUID entityId, ActionType actionType,
                                                 ExecutionStatus status, String resultText, String errorMessage,
                                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new WorkflowExecution(id, companyId, workflowId, workflowActionId, eventId, eventType,
                entityId, actionType, status, resultText, errorMessage, createdAt, updatedAt);
    }

    public void markSuccess(String resultText) {
        this.status = ExecutionStatus.SUCCESS;
        this.resultText = resultText;
        this.updatedAt = LocalDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.status = ExecutionStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }

    public void markSkipped() {
        this.status = ExecutionStatus.SKIPPED;
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getWorkflowId() { return workflowId; }
    public UUID getWorkflowActionId() { return workflowActionId; }
    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public UUID getEntityId() { return entityId; }
    public ActionType getActionType() { return actionType; }
    public ExecutionStatus getStatus() { return status; }
    public String getResultText() { return resultText; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}