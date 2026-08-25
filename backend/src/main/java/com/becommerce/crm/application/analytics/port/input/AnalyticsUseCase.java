package com.becommerce.crm.application.analytics.port.input;

import com.becommerce.crm.application.analytics.AnalyticsPeriod;
import com.becommerce.crm.application.analytics.dto.AnalyticsSummaryResponse;

import java.util.UUID;

/** Caso de uso de Analytics (Sprint 19) — somente leitura. */
public interface AnalyticsUseCase {

    AnalyticsSummaryResponse summary(UUID companyId, AnalyticsPeriod period);
}
