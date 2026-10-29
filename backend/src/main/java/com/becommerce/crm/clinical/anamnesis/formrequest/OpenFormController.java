package com.becommerce.crm.clinical.anamnesis.formrequest;

import com.becommerce.crm.clinical.anamnesis.formrequest.PatientFormDtos.OpenLinkResponse;
import com.becommerce.crm.clinical.anamnesis.formrequest.PatientFormDtos.PublicFormResponse;
import com.becommerce.crm.clinical.anamnesis.formrequest.PatientFormDtos.SubmitOpenForm;
import com.becommerce.crm.masterdata.contact.registration.domain.exception.RegistrationRateLimitException;
import com.becommerce.crm.masterdata.contact.registration.infrastructure.rate.RegistrationRateLimiter;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import jakarta.validation.Valid;
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
 * Link aberto do formulário: gerar/copiar na lista de modelos (autenticado) e as
 * rotas públicas {@code /api/v1/public/forms/open/{token}} (sem login, com o
 * mesmo limite por IP do formulário por link).
 */
@RestController
@RequestMapping("/api/v1")
public class OpenFormController {

    private static final String OWN_COMPANY = "Você só pode acessar modelos da sua própria empresa.";

    private final OpenFormService service;
    private final RegistrationRateLimiter rateLimiter;

    public OpenFormController(OpenFormService service, RegistrationRateLimiter rateLimiter) {
        this.service = service;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/companies/{companyId}/anamnesis-models/{modelId}/open-link")
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<OpenLinkResponse> openLink(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                                     @PathVariable UUID modelId,
                                                     @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(service.getOrCreate(companyId, modelId, principal.userId()));
    }

    @GetMapping("/public/forms/open/{token}")
    public ResponseEntity<PublicFormResponse> preview(
            @PathVariable String token,
            @RequestHeader(value = "X-Real-IP", required = false) String realIp) {
        checkRate(realIp);
        return ResponseEntity.ok(service.preview(token));
    }

    @PostMapping("/public/forms/open/{token}")
    public ResponseEntity<Void> submit(
            @PathVariable String token,
            @Valid @RequestBody SubmitOpenForm form,
            @RequestHeader(value = "X-Real-IP", required = false) String realIp) {
        checkRate(realIp);
        service.submit(token, form);
        return ResponseEntity.noContent().build();
    }

    private void checkRate(String realIp) {
        String key = realIp != null && !realIp.isBlank() ? realIp : "unknown";
        if (!rateLimiter.tryAcquire(key)) {
            throw new RegistrationRateLimitException();
        }
    }
}
