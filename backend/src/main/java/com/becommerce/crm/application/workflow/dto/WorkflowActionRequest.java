package com.becommerce.crm.application.workflow.dto;

import com.becommerce.crm.domain.workflow.ActionType;

import java.util.Map;
import java.util.UUID;

public record WorkflowActionRequest(
        UUID id,
        ActionType actionType,
        int sortOrder,
        Map<String, Object> config
) {
}
