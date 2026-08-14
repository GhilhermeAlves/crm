package com.becommerce.crm.application.workflow.dto;

import com.becommerce.crm.domain.workflow.ActionType;
import com.becommerce.crm.domain.workflow.ExecutionStatus;

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
