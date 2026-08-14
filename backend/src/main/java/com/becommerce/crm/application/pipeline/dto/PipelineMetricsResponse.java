package com.becommerce.crm.application.pipeline.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Métricas de um pipeline (P-030..P-033): faturamento por estágio, ganho,
 *  perda, win rate, ciclo médio e forecast com base na probabilidade. */
public record PipelineMetricsResponse(
        UUID pipelineId,
        int openCount,
        int wonCount,
        int lostCount,
        BigDecimal totalValue,
        BigDecimal wonValue,
        BigDecimal lostValue,
        BigDecimal winRate,
        Double averageCycleDays,
        BigDecimal forecast,
        List<StageMetric> byStage
) {
    public record StageMetric(
            UUID stageId,
            String stageName,
            int count,
            BigDecimal value
    ) {}
}
