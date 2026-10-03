package com.becommerce.crm.analytics.reporting.application.port.in;

import com.becommerce.crm.analytics.reporting.application.AnalyticsPeriod;
import com.becommerce.crm.analytics.reporting.application.dto.AnalyticsSummaryResponse;

import java.util.UUID;

/** Caso de uso de Analytics (Sprint 19) — somente leitura. */
public interface AnalyticsUseCase {

    AnalyticsSummaryResponse summary(UUID companyId, AnalyticsPeriod period);
}
