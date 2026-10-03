package com.becommerce.crm.automation.workflow.application.dto;

import com.becommerce.crm.automation.workflow.domain.ActionType;

import java.util.Map;
import java.util.UUID;

public record WorkflowActionRequest(
        UUID id,
        ActionType actionType,
        int sortOrder,
        Map<String, Object> config
) {
}
