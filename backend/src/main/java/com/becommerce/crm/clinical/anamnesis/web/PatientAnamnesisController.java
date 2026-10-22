package com.becommerce.crm.clinical.anamnesis.web;

import com.becommerce.crm.clinical.anamnesis.application.dto.PatientAnamnesisResponse;
import com.becommerce.crm.clinical.anamnesis.application.dto.SavePatientAnamnesisRequest;
import com.becommerce.crm.clinical.anamnesis.application.dto.StartPatientAnamnesisRequest;
import com.becommerce.crm.clinical.anamnesis.application.port.in.PatientAnamnesisUseCase;
import com.becommerce.crm.identity.membership.domain.Membership;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Anamnese preenchida do paciente (ficha do paciente, Fase 2). Scoped à empresa
 * ativa via {@link CurrentCompanyId}; leitura com {@code anamnesis:read} e
 * escrita com {@code anamnesis:manage}.
 */
@RestController
public class PatientAnamnesisController {

    private static final String ACCESS_MESSAGE =
            "Você só pode acessar anamneses da sua própria empresa.";

    private final PatientAnamnesisUseCase useCase;

    public PatientAnamnesisController(PatientAnamnesisUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/api/v1/companies/{companyId}/patients/{contactId}/anamneses")
    @PreAuthorize("hasAuthority('contact:read')")
    public ResponseEntity<List<PatientAnamnesisResponse>> list(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @PathVariable UUID contactId) {
        return ResponseEntity.ok(useCase.list(companyId, contactId));
    }

    @GetMapping("/api/v1/companies/{companyId}/patients/{contactId}/anamneses/current")
    @PreAuthorize("hasAuthority('contact:read')")
    public ResponseEntity<PatientAnamnesisResponse> current(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @PathVariable UUID contactId) {
        return useCase.current(companyId, contactId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/api/v1/companies/{companyId}/patients/{contactId}/anamneses/{anamnesisId}")
    @PreAuthorize("hasAuthority('contact:read')")
    public ResponseEntity<PatientAnamnesisResponse> get(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @PathVariable UUID contactId,
            @PathVariable UUID anamnesisId) {
        return ResponseEntity.ok(useCase.get(companyId, contactId, anamnesisId));
    }

    @PostMapping("/api/v1/companies/{companyId}/patients/{contactId}/anamneses")
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<PatientAnamnesisResponse> start(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @PathVariable UUID contactId,
            @Valid @RequestBody(required = false) StartPatientAnamnesisRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(useCase.start(companyId, contactId, request, principal.userId()));
    }

    @PutMapping("/api/v1/companies/{companyId}/patients/{contactId}/anamneses/{anamnesisId}")
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<PatientAnamnesisResponse> save(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @PathVariable UUID contactId,
            @PathVariable UUID anamnesisId,
            @Valid @RequestBody SavePatientAnamnesisRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(useCase.save(companyId, contactId, anamnesisId, request,
                principal.userId(), isAdmin(principal)));
    }

    @DeleteMapping("/api/v1/companies/{companyId}/patients/{contactId}/anamneses/{anamnesisId}")
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<Void> delete(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @PathVariable UUID contactId,
            @PathVariable UUID anamnesisId,
            @AuthenticationPrincipal CurrentUser principal) {
        useCase.delete(companyId, contactId, anamnesisId, principal.userId(), isAdmin(principal));
        return ResponseEntity.noContent().build();
    }

    private static boolean isAdmin(CurrentUser principal) {
        return Membership.isAdminLevelRole(principal.membershipRole())
                || principal.roles().stream().anyMatch(Membership::isAdminLevelRole);
    }
}
