package com.becommerce.crm.domain.workflow;

import java.util.UUID;

/**
 * Ação de um workflow (Item 4). {@code config} é um payload JSON específico do
 * tipo de ação (ex.: CREATE_TASK → {@code {"title":..., "dueInDays": 2,
 * "priority": "HIGH"}}). Mantido como texto para permitir evolução de tipos
 * (Item 15: SendEmail, WhatsApp, Webhook, AI) sem reconstrução.
 */
public class WorkflowAction {

    private final UUID id;
    private final UUID companyId;
    private final UUID workflowId;
    private final ActionType actionType;
    private final int sortOrder;
    private final String config;

    private WorkflowAction(UUID id, UUID companyId, UUID workflowId, ActionType actionType,
                           int sortOrder, String config) {
        this.id = id;
        this.companyId = companyId;
        this.workflowId = workflowId;
        this.actionType = actionType;
        this.sortOrder = sortOrder;
        this.config = config;
    }

    public static WorkflowAction create(UUID companyId, UUID workflowId, ActionType actionType,
                                        int sortOrder, String config) {
        return new WorkflowAction(UUID.randomUUID(), companyId, workflowId, actionType, sortOrder, config);
    }

    public static WorkflowAction reconstitute(UUID id, UUID companyId, UUID workflowId, ActionType actionType,
                                              int sortOrder, String config) {
        return new WorkflowAction(id, companyId, workflowId, actionType, sortOrder, config);
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getWorkflowId() { return workflowId; }
    public ActionType getActionType() { return actionType; }
    public int getSortOrder() { return sortOrder; }
    public String getConfig() { return config; }
}