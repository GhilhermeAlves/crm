package com.becommerce.crm.billing.web;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.billing.application.BillingRuleException;
import com.becommerce.crm.billing.application.BillingService;
import com.becommerce.crm.billing.application.BillingService.BillingView;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Cobrança das empresas — só o administrador do CRM. */
@RestController
@RequestMapping("/api/v1/admin/billing/companies/{companyId}")
public class BillingAdminController {

    private final BillingService service;
    private final TenantAuditRecorder auditor;

    private com.becommerce.crm.finance.patientbilling.PatientBillingService patientBilling;

    public BillingAdminController(BillingService service, TenantAuditRecorder auditor) {
        this.service = service;
        this.auditor = auditor;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setPatientBilling(com.becommerce.crm.finance.patientbilling.PatientBillingService patientBilling) {
        this.patientBilling = patientBilling;
    }

    public record PatientBillingRequest(boolean enabled) {}

    /** Liga/desliga a cobrança de pacientes pelo Asaas nesta empresa (só o dono do CRM). */
    @PutMapping("/patient-billing")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Boolean> patientBilling(@PathVariable UUID companyId, @RequestBody PatientBillingRequest body,
                                               @AuthenticationPrincipal CurrentUser user) {
        requireCrmAdmin(user);
        if (patientBilling == null) throw new BillingRuleException("Cobrança de pacientes indisponível.");
        patientBilling.setEnabled(companyId, body.enabled());
        inTenant(companyId, () -> auditor.record(companyId, AuditAction.UPDATE, AuditModule.SETTINGS, "Billing",
                companyId.toString(), "Cobrança de pacientes pelo Asaas " + (body.enabled() ? "ligada" : "desligada"),
                user.userId(), null));
        return Map.of("enabled", patientBilling.enabled(companyId));
    }

    @GetMapping("/patient-billing")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Boolean> patientBillingStatus(@PathVariable UUID companyId,
                                                     @AuthenticationPrincipal CurrentUser user) {
        requireCrmAdmin(user);
        return Map.of("enabled", patientBilling != null && patientBilling.enabled(companyId));
    }

    public record PriceRequest(@Positive int monthlyPriceCents) {}

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public BillingView get(@PathVariable UUID companyId, @AuthenticationPrincipal CurrentUser user) {
        requireCrmAdmin(user);
        return service.view(companyId);
    }

    @PostMapping("/activate")
    @PreAuthorize("isAuthenticated()")
    public BillingView activate(@PathVariable UUID companyId, @AuthenticationPrincipal CurrentUser user) {
        requireCrmAdmin(user);
        return service.activate(companyId);
    }

    @PostMapping("/exempt")
    @PreAuthorize("isAuthenticated()")
    public BillingView exempt(@PathVariable UUID companyId, @AuthenticationPrincipal CurrentUser user) {
        requireCrmAdmin(user);
        BillingView view = service.markExempt(companyId);
        inTenant(companyId, () -> auditor.record(companyId, AuditAction.UPDATE, AuditModule.SETTINGS, "Billing",
                companyId.toString(), "Cobrança: empresa marcada como isenta", user.userId(), null));
        return view;
    }

    @PostMapping("/cancel")
    @PreAuthorize("isAuthenticated()")
    public BillingView cancel(@PathVariable UUID companyId, @AuthenticationPrincipal CurrentUser user) {
        requireCrmAdmin(user);
        BillingView view = service.cancel(companyId);
        inTenant(companyId, () -> auditor.record(companyId, AuditAction.UPDATE, AuditModule.SETTINGS, "Billing",
                companyId.toString(), "Cobrança: assinatura encerrada", user.userId(), null));
        return view;
    }

    @PutMapping("/price")
    @PreAuthorize("isAuthenticated()")
    public BillingView price(@PathVariable UUID companyId, @Valid @RequestBody PriceRequest body,
                             @AuthenticationPrincipal CurrentUser user) {
        requireCrmAdmin(user);
        BillingView view = service.changePrice(companyId, body.monthlyPriceCents());
        inTenant(companyId, () -> auditor.record(companyId, AuditAction.UPDATE, AuditModule.SETTINGS, "Billing",
                companyId.toString(), "Cobrança: valor mensal alterado", user.userId(),
                Map.of("monthlyPriceCents", body.monthlyPriceCents())));
        return view;
    }

    public record GrantRequest(@jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(30) int days) {}

    @PostMapping("/grant")
    @PreAuthorize("isAuthenticated()")
    public BillingView grant(@PathVariable UUID companyId, @Valid @RequestBody GrantRequest body,
                             @AuthenticationPrincipal CurrentUser user) {
        requireCrmAdmin(user);
        BillingView view = service.grant(companyId, body.days());
        inTenant(companyId, () -> auditor.record(companyId, AuditAction.UPDATE, AuditModule.SETTINGS, "Billing",
                companyId.toString(), "Cobrança: liberada por " + body.days() + " dias", user.userId(),
                Map.of("days", body.days())));
        return view;
    }

    @GetMapping("/invoices")
    @PreAuthorize("isAuthenticated()")
    public List<BillingService.InvoiceView> invoices(@PathVariable UUID companyId,
                                                     @AuthenticationPrincipal CurrentUser user) {
        requireCrmAdmin(user);
        return service.invoices(companyId);
    }

    @ExceptionHandler(BillingRuleException.class)
    public ResponseEntity<Map<String, String>> rule(BillingRuleException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, String>> concurrentChange() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "A cobrança mudou ao mesmo tempo; tente de novo."));
    }

    /**
     * A auditoria grava em audit_logs (RLS por tenant): o tenant precisa ser o da empresa alvo,
     * não o da empresa do SUPER_ADMIN. Troca temporariamente e restaura em finally.
     */
    private static void inTenant(UUID companyId, Runnable work) {
        UUID previous = TenantContext.getCompanyId();
        boolean swap = previous == null || !previous.equals(companyId);
        try {
            if (swap) TenantContext.setCompanyId(companyId);
            work.run();
        } finally {
            if (swap) {
                if (previous == null) TenantContext.clear(); else TenantContext.setCompanyId(previous);
            }
        }
    }

    private static void requireCrmAdmin(CurrentUser user) {
        if (user == null || !user.isPlatformAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Só o dono do CRM gerencia a cobrança.");
        }
    }
}
