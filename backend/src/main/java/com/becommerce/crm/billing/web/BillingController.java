package com.becommerce.crm.billing.web;

import com.becommerce.crm.billing.application.BillingService;
import com.becommerce.crm.billing.application.BillingService.BillingView;
import com.becommerce.crm.billing.application.BillingService.InvoiceView;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

/** Cobrança da empresa logada. Liberado pelo BillingGuardInterceptor mesmo com a empresa bloqueada. */
@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private final BillingService service;

    public BillingController(BillingService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public BillingView view(@AuthenticationPrincipal CurrentUser user) {
        return service.view(companyOf(user));
    }

    @GetMapping("/invoices")
    @PreAuthorize("isAuthenticated()")
    public List<InvoiceView> invoices(@AuthenticationPrincipal CurrentUser user) {
        return service.invoices(companyOf(user));
    }

    @PostMapping("/check-payment")
    @PreAuthorize("isAuthenticated()")
    public BillingView checkPayment(@AuthenticationPrincipal CurrentUser user) {
        return service.checkPayment(companyOf(user));
    }

    private static UUID companyOf(CurrentUser user) {
        if (user == null || user.companyId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário sem empresa.");
        }
        return user.companyId();
    }
}
