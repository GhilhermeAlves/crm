package com.becommerce.crm.application.workflow.dto;

import com.becommerce.crm.domain.workflow.WorkflowRunStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Detalhe de uma execução de regra (Sprint 15): além dos dados gerais, traz as
 * condições avaliadas (esperado × encontrado), o contexto seguro do evento e as
 * ações executadas (do histórico {@code workflow_executions}).
 */
public record WorkflowRunDetailResponse(
        UUID id,
        UUID workflowId,
        String eventType,
        UUID entityId,
        WorkflowRunStatus status,
        String resultText,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ConditionEvaluation> conditions,
        Map<String, Object> context,
        List<WorkflowExecutionResponse> actions
) {
}
