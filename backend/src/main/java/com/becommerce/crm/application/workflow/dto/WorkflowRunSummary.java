package com.becommerce.crm.application.workflow.dto;

import com.becommerce.crm.domain.workflow.WorkflowRunStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resumo por workflow para a lista (Sprint 15): total de execuções, última
 * execução (status/data) e último erro, quando houver.
 */
public record WorkflowRunSummary(
        UUID workflowId,
        long runCount,
        WorkflowRunStatus lastStatus,
        LocalDateTime lastAt,
        String lastError
) {
}
