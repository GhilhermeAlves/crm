package com.becommerce.crm.application.workflow.dto;

import com.becommerce.crm.domain.workflow.ConditionOperator;

import java.util.UUID;

public record WorkflowConditionRequest(
        UUID id,
        String field,
        ConditionOperator operator,
        String value,
        int sortOrder
) {
}
