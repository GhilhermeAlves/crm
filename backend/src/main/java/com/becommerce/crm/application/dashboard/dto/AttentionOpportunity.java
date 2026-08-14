package com.becommerce.crm.application.dashboard.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Oportunidade que merece atenção (inteligência operacional determinística,
 * Sprint 12). Regras: sem atividade há N dias (parada) e/ou alta prioridade
 * (score ponderado por valor, probabilidade e tempo de inatividade).
 */
public record AttentionOpportunity(
        UUID id,
        String title,
        BigDecimal value,
        String contactName,
        String stageName,
        int stageOrder,
        String pipelineName,
        boolean stale,
        long daysInactive,
        String suggestion,
        int priorityScore
) {
}