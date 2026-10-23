package com.becommerce.crm.masterdata.contact.registration.web;

import com.becommerce.crm.masterdata.contact.registration.application.dto.PatientRegistrationRequest;
import com.becommerce.crm.masterdata.contact.registration.application.dto.RegistrationLinkResponse;
import com.becommerce.crm.masterdata.contact.registration.application.dto.RegistrationPreviewResponse;
import com.becommerce.crm.masterdata.contact.registration.application.service.RegistrationLinkService;
import com.becommerce.crm.masterdata.contact.registration.domain.exception.RegistrationRateLimitException;
import com.becommerce.crm.masterdata.contact.registration.infrastructure.rate.RegistrationRateLimiter;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Link de cadastro do paciente: geração (autenticada, {@code contact:update}) e
 * rotas públicas {@code /api/v1/public/registration/{token}} (sem login; nginx
 * manda direto ao backend com X-Real-IP, usado no limite por IP).
 */
@RestController
@RequestMapping("/api/v1")
public class RegistrationLinkController {

    private static final String OWN_COMPANY = "Você só pode acessar pacientes da sua própria empresa.";

    private final RegistrationLinkService service;
    private final RegistrationRateLimiter rateLimiter;

    public RegistrationLinkController(RegistrationLinkService service, RegistrationRateLimiter rateLimiter) {
        this.service = service;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/companies/{companyId}/patients/{contactId}/registration-link")
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<RegistrationLinkResponse> create(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @PathVariable UUID contactId,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(companyId, contactId, principal.userId()));
    }

    @GetMapping("/public/registration/{token}")
    public ResponseEntity<RegistrationPreviewResponse> preview(
            @PathVariable String token,
            @RequestHeader(value = "X-Real-IP", required = false) String realIp) {
        checkRate(realIp);
        return ResponseEntity.ok(service.preview(token));
    }

    @PostMapping("/public/registration/{token}")
    public ResponseEntity<Void> submit(
            @PathVariable String token,
            @Valid @RequestBody PatientRegistrationRequest request,
            @RequestHeader(value = "X-Real-IP", required = false) String realIp) {
        checkRate(realIp);
        service.submit(token, request);
        return ResponseEntity.noContent().build();
    }

    private void checkRate(String realIp) {
        String key = realIp != null && !realIp.isBlank() ? realIp : "unknown";
        if (!rateLimiter.tryAcquire(key)) {
            throw new RegistrationRateLimitException();
        }
    }
}
