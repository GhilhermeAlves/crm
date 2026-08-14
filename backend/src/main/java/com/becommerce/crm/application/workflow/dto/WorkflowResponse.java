package com.becommerce.crm.application.workflow.dto;

import com.becommerce.crm.domain.workflow.ActionType;
import com.becommerce.crm.domain.workflow.ConditionOperator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record WorkflowResponse(
        UUID id,
        UUID companyId,
        String name,
        String description,
        String trigger,
        boolean active,
        List<ConditionResponse> conditions,
        List<ActionResponse> actions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record ConditionResponse(
            UUID id,
            String field,
            ConditionOperator operator,
            String value,
            int sortOrder
    ) {
    }

    public record ActionResponse(
            UUID id,
            ActionType actionType,
            int sortOrder,
            String config
    ) {
    }
}
