package com.becommerce.crm.automation.workflow.application.dto;

import com.becommerce.crm.automation.workflow.domain.ActionType;
import com.becommerce.crm.automation.workflow.domain.ExecutionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record WorkflowExecutionResponse(
        UUID id,
        UUID workflowId,
        ActionType actionType,
        String eventType,
        UUID entityId,
        ExecutionStatus status,
        String resultText,
        String errorMessage,
        LocalDateTime createdAt
) {
}
