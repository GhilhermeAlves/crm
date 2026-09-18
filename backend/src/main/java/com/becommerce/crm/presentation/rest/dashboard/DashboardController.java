package com.becommerce.crm.presentation.rest.dashboard;

import com.becommerce.crm.application.dashboard.dto.OperationalDashboard;
import com.becommerce.crm.application.dashboard.service.DashboardService;
import com.becommerce.crm.infrastructure.security.config.CurrentCompanyId;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Dashboard orientado à ação (Sprint 12, ITEM 4). Exige a permissão
 * {@code dashboard:operational} e é scoped à empresa ativa.
 */
@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/v1/companies/{companyId}/dashboard/operational")
    @PreAuthorize("hasAuthority('dashboard:operational')")
    public ResponseEntity<OperationalDashboard> operational(
            @CurrentCompanyId("Você só pode acessar o dashboard da sua própria empresa.") UUID companyId) {
        return ResponseEntity.ok(dashboardService.build(companyId));
    }
}