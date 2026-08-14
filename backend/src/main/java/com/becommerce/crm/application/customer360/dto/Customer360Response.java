package com.becommerce.crm.application.customer360.dto;

/** Resposta consolidada do Customer 360 de um contato. */
public record Customer360Response(
        java.util.UUID companyId,
        ContactSummaryResponse contact,
        int openOpportunities,
        java.math.BigDecimal openValue,
        java.util.List<OpportunityItemResponse> opportunities,
        java.util.List<TaskItemResponse> tasks,
        java.util.List<TimelineEventResponse> timeline,
        NextActionResponse nextAction
) {}