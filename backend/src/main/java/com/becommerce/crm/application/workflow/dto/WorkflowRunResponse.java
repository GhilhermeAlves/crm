package com.becommerce.crm.application.workflow.dto;

import com.becommerce.crm.domain.workflow.WorkflowRunStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Execução de regra (workflow_run) — item de lista do histórico (Sprint 15).
 */
public record WorkflowRunResponse(
        UUID id,
        UUID workflowId,
        String eventType,
        UUID entityId,
        WorkflowRunStatus status,
        String resultText,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
