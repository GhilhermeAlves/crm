package com.becommerce.crm.application.dashboard.dto;

import com.becommerce.crm.application.activity.dto.ActivityResponse;
import com.becommerce.crm.application.task.dto.TaskResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dashboard orientado à ação (Sprint 12) — resposta à pergunta "o que merece
 * minha atenção hoje?". Agrega métricas determinísticas (ITEMS 3 e 4) sem IA.
 */
public record OperationalDashboard(
        String greeting,
        long opportunitiesNeedingAttention,
        long staleOpportunities,
        long tasksDueToday,
        long openOpportunities,
        BigDecimal openValue,
        List<AttentionOpportunity> attentionOpportunities,
        List<TaskResponse> dueToday,
        List<ActivityResponse> recentActivities
) {
}