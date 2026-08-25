package com.becommerce.crm.application.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Read model agregado do dashboard (Sprint 19). Todos os valores vêm de
 * consultas SQL sobre as tabelas operacionais existentes — nenhum dado é
 * copiado/duplicado. {@code current} cobre o período selecionado e
 * {@code previous} o período imediatamente anterior (mesma duração).
 */
public record AnalyticsSummaryResponse(
        String from,
        String to,
        Metrics current,
        Metrics previous,
        List<DailyPoint> series
) {

    public record Metrics(
            long contactsCreated,
            long leadsCreated,
            long leadsConverted,
            long opportunitiesCreated,
            long opportunitiesWon,
            BigDecimal wonValue,
            BigDecimal pipelineOpenValue,
            long activitiesCreated,
            long tasksCreated,
            long tasksCompleted,
            long tasksOverdue,
            long campaignsExecuted,
            long campaignMessagesSent,
            long campaignMessagesFailed,
            long omnichannelMessagesIn,
            long omnichannelMessagesOut,
            long workflowRunsMatched,
            long workflowRunsSuccess,
            long workflowRunsFailed
    ) {
    }

    /** Série diária (dia no timezone da consulta). */
    public record DailyPoint(String date, long leads, long opportunities, long messagesSent) {
    }
}
