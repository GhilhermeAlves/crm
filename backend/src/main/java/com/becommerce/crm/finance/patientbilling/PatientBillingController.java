package com.becommerce.crm.finance.patientbilling;

import com.becommerce.crm.finance.patientbilling.PatientBillingService.ChargeView;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Cobrança de pacientes pelo Asaas, na empresa do usuário logado. */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/patient-billing")
public class PatientBillingController {

    private static final String OWN_COMPANY = "Você só pode acessar o financeiro da sua própria empresa.";
    private final PatientBillingService service;

    public PatientBillingController(PatientBillingService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('finance:read')")
    public Map<String, Boolean> status(@CurrentCompanyId(OWN_COMPANY) UUID companyId) {
        return Map.of("enabled", service.enabled(companyId));
    }

    @GetMapping("/charges")
    @PreAuthorize("hasAuthority('finance:read')")
    public List<ChargeView> charges(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                    @RequestParam(name = "transactionIds", required = false) List<UUID> ids) {
        return ids == null ? List.of() : service.charges(companyId, ids);
    }

    @PostMapping("/transactions/{transactionId}/charge")
    @PreAuthorize("hasAuthority('finance:manage')")
    public ChargeView charge(@CurrentCompanyId(OWN_COMPANY) UUID companyId, @PathVariable UUID transactionId) {
        return service.charge(companyId, transactionId);
    }

    @PostMapping("/transactions/{transactionId}/whatsapp")
    @PreAuthorize("hasAuthority('finance:manage')")
    public ChargeView whatsapp(@CurrentCompanyId(OWN_COMPANY) UUID companyId, @PathVariable UUID transactionId) {
        return service.resendWhatsApp(companyId, transactionId);
    }

    @ExceptionHandler(PatientBillingException.class)
    public ResponseEntity<Map<String, String>> rule(PatientBillingException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}
