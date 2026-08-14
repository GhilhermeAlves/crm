package com.becommerce.crm.domain.workflow;

import java.util.UUID;

/**
 * Condição de um workflow (Item 3): compara um {@code field} do contexto do
 * evento usando {@code operator} contra {@code value}. Fields suportados na
 * primeira versão: {@code opportunity.stage}, {@code opportunity.value},
 * {@code task.priority}, {@code activity.type}.
 */
public class WorkflowCondition {

    private final UUID id;
    private final UUID companyId;
    private final UUID workflowId;
    private final String field;
    private final ConditionOperator operator;
    private final String value;
    private final int sortOrder;

    private WorkflowCondition(UUID id, UUID companyId, UUID workflowId, String field,
                              ConditionOperator operator, String value, int sortOrder) {
        this.id = id;
        this.companyId = companyId;
        this.workflowId = workflowId;
        this.field = field;
        this.operator = operator;
        this.value = value;
        this.sortOrder = sortOrder;
    }

    public static WorkflowCondition create(UUID companyId, UUID workflowId, String field,
                                           ConditionOperator operator, String value, int sortOrder) {
        return new WorkflowCondition(UUID.randomUUID(), companyId, workflowId, field, operator, value, sortOrder);
    }

    public static WorkflowCondition reconstitute(UUID id, UUID companyId, UUID workflowId, String field,
                                                 ConditionOperator operator, String value, int sortOrder) {
        return new WorkflowCondition(id, companyId, workflowId, field, operator, value, sortOrder);
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getWorkflowId() { return workflowId; }
    public String getField() { return field; }
    public ConditionOperator getOperator() { return operator; }
    public String getValue() { return value; }
    public int getSortOrder() { return sortOrder; }
}