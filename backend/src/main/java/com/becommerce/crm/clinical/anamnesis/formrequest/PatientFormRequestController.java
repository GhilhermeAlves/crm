package com.becommerce.crm.clinical.anamnesis.formrequest;

import com.becommerce.crm.clinical.anamnesis.formrequest.PatientFormDtos.CreateFormRequest;
import com.becommerce.crm.clinical.anamnesis.formrequest.PatientFormDtos.CreatedFormRequest;
import com.becommerce.crm.clinical.anamnesis.formrequest.PatientFormDtos.FormRequestResponse;
import com.becommerce.crm.clinical.anamnesis.formrequest.PatientFormDtos.PublicFormResponse;
import com.becommerce.crm.clinical.anamnesis.formrequest.PatientFormDtos.SubmitPublicForm;
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

import java.util.List;
import java.util.UUID;

/**
 * Formulário respondido pelo paciente: envio e lista na ficha (autenticados) e
 * rotas públicas {@code /api/v1/public/forms/{token}} (sem login; o nginx manda
 * direto ao backend com X-Real-IP, usado no limite por IP). Cancelar = descartar
 * a anamnese em rascunho (o pedido some junto, FK em cascata).
 */
@RestController
@RequestMapping("/api/v1")
public class PatientFormRequestController {

    private static final String OWN_COMPANY = "Você só pode acessar pacientes da sua própria empresa.";

    private final PatientFormRequestService service;
    private final RegistrationRateLimiter rateLimiter;

    public PatientFormRequestController(PatientFormRequestService service, RegistrationRateLimiter rateLimiter) {
        this.service = service;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/companies/{companyId}/patients/{contactId}/form-requests")
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<CreatedFormRequest> create(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @PathVariable UUID contactId,
            @Valid @RequestBody CreateFormRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(companyId, contactId, request, principal.userId()));
    }

    @GetMapping("/companies/{companyId}/form-requests/options")
    @PreAuthorize("hasAuthority('contact:read')")
    public ResponseEntity<PatientFormDtos.SendOptions> options(@CurrentCompanyId(OWN_COMPANY) UUID companyId) {
        return ResponseEntity.ok(service.sendOptions());
    }

    @GetMapping("/companies/{companyId}/patients/{contactId}/form-requests")
    @PreAuthorize("hasAuthority('contact:read')")
    public ResponseEntity<List<FormRequestResponse>> list(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @PathVariable UUID contactId) {
        return ResponseEntity.ok(service.list(companyId, contactId));
    }

    @GetMapping("/public/forms/{token}")
    public ResponseEntity<PublicFormResponse> preview(
            @PathVariable String token,
            @RequestHeader(value = "X-Real-IP", required = false) String realIp) {
        checkRate(realIp);
        return ResponseEntity.ok(service.preview(token));
    }

    @PostMapping("/public/forms/{token}")
    public ResponseEntity<Void> submit(
            @PathVariable String token,
            @Valid @RequestBody SubmitPublicForm form,
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
