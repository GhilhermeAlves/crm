package com.becommerce.crm.domain.workflow;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro de execução de uma RULE para um evento (Sprint 15). Uma linha por
 * {@code (company, workflow, event)} — distinto de {@link WorkflowExecution}
 * (que é por ação). Guarda, em JSON, as condições avaliadas (esperado ×
 * encontrado) e o contexto seguro do evento, permitindo responder "por que esta
 * regra executou / não executou?".
 */
public class WorkflowRun {

    private final UUID id;
    private final UUID companyId;
    private final UUID workflowId;
    private final UUID eventId;
    private final String eventType;
    private final UUID entityId;
    private WorkflowRunStatus status;
    private final String conditions;
    private final String context;
    private String resultText;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private WorkflowRun(UUID id, UUID companyId, UUID workflowId, UUID eventId, String eventType,
                        UUID entityId, WorkflowRunStatus status, String conditions, String context,
                        String resultText, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.workflowId = workflowId;
        this.eventId = eventId;
        this.eventType = eventType;
        this.entityId = entityId;
        this.status = status;
        this.conditions = conditions;
        this.context = context;
        this.resultText = resultText;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static WorkflowRun reconstitute(UUID id, UUID companyId, UUID workflowId, UUID eventId,
                                           String eventType, UUID entityId, WorkflowRunStatus status,
                                           String conditions, String context, String resultText,
                                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new WorkflowRun(id, companyId, workflowId, eventId, eventType, entityId,
                status, conditions, context, resultText, createdAt, updatedAt);
    }

    public void updateStatus(WorkflowRunStatus newStatus, String resultText) {
        this.status = newStatus;
        this.resultText = resultText;
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getWorkflowId() { return workflowId; }
    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public UUID getEntityId() { return entityId; }
    public WorkflowRunStatus getStatus() { return status; }
    public String getConditions() { return conditions; }
    public String getContext() { return context; }
    public String getResultText() { return resultText; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
