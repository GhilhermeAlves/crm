package com.becommerce.crm.infrastructure.workflow.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workflow_executions")
public class WorkflowExecutionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "workflow_id")
    private UUID workflowId;

    @Column(name = "workflow_action_id")
    private UUID workflowActionId;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "action_type")
    private String actionType;

    @Column(name = "status")
    private String status;

    @Column(name = "result_text")
    private String resultText;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getWorkflowId() { return workflowId; }
    public void setWorkflowId(UUID workflowId) { this.workflowId = workflowId; }
    public UUID getWorkflowActionId() { return workflowActionId; }
    public void setWorkflowActionId(UUID workflowActionId) { this.workflowActionId = workflowActionId; }
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getResultText() { return resultText; }
    public void setResultText(String resultText) { this.resultText = resultText; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}