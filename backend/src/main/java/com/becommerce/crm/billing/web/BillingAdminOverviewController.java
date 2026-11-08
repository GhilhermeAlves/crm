package com.becommerce.crm.billing.web;

import com.becommerce.crm.billing.application.BillingOverviewService;
import com.becommerce.crm.billing.application.BillingOverviewService.CompanyBillingStatus;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
public class BillingAdminOverviewController {

    private final BillingOverviewService service;

    public BillingAdminOverviewController(BillingOverviewService service) { this.service = service; }

    @GetMapping("/api/v1/admin/billing/companies")
    @PreAuthorize("isAuthenticated()")
    public List<CompanyBillingStatus> statuses(@AuthenticationPrincipal CurrentUser user) {
        if (user == null || !user.isCrmAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Só o administrador do CRM gerencia a cobrança.");
        }
        return service.statuses();
    }
}
