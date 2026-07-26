package com.becommerce.crm.presentation.rest.dashboard;

import com.becommerce.crm.application.dashboard.dto.DashboardKpisResponse;
import com.becommerce.crm.application.dashboard.dto.RecentActivityResponse;
import com.becommerce.crm.application.dashboard.port.input.DashboardUseCase;
import com.becommerce.crm.infrastructure.security.filter.CrmPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardUseCase dashboardUseCase;

    public DashboardController(DashboardUseCase dashboardUseCase) {
        this.dashboardUseCase = dashboardUseCase;
    }

    @GetMapping("/kpis")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public ResponseEntity<DashboardKpisResponse> getKpis(@AuthenticationPrincipal CrmPrincipal principal) {
        return ResponseEntity.ok(dashboardUseCase.getKpis(principal.companyId()));
    }

    @GetMapping("/recent-activities")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public ResponseEntity<List<RecentActivityResponse>> getRecentActivities(@AuthenticationPrincipal CrmPrincipal principal) {
        return ResponseEntity.ok(dashboardUseCase.getRecentActivities(principal.companyId()));
    }
}
