package com.becommerce.crm.automation.workflow.application.dto;

import com.becommerce.crm.automation.workflow.domain.ConditionOperator;

import java.util.UUID;

public record WorkflowConditionRequest(
        UUID id,
        String field,
        ConditionOperator operator,
        String value,
        int sortOrder
) {
}
