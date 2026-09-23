package com.becommerce.crm.presentation.rest.analytics;

import com.becommerce.crm.application.analytics.AnalyticsPeriod;
import com.becommerce.crm.application.analytics.dto.AnalyticsSummaryResponse;
import com.becommerce.crm.application.analytics.port.input.AnalyticsUseCase;
import com.becommerce.crm.infrastructure.security.config.CurrentCompanyId;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Analytics read-only (Sprint 19). Endpoint agregado único para o dashboard
 * (reduz round trips). Isolamento: checagem central de tenant (@CurrentCompanyId)
 * + TenantContext + RLS FORCE nas tabelas consultadas.
 */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/analytics")
public class AnalyticsController {

    private final AnalyticsUseCase analyticsUseCase;

    public AnalyticsController(AnalyticsUseCase analyticsUseCase) {
        this.analyticsUseCase = analyticsUseCase;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('analytics:read')")
    public ResponseEntity<AnalyticsSummaryResponse> summary(
            @CurrentCompanyId("Você só pode acessar analytics da sua própria empresa.") UUID companyId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String timezone) {
        AnalyticsPeriod period = AnalyticsPeriod.resolve(from, to, timezone);
        return ResponseEntity.ok(analyticsUseCase.summary(companyId, period));
    }
}