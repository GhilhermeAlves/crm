package com.becommerce.crm.clinical.evolution.web;

import com.becommerce.crm.clinical.evolution.application.dto.PatientEvolutionRequest;
import com.becommerce.crm.clinical.evolution.application.dto.PatientEvolutionResponse;
import com.becommerce.crm.clinical.evolution.application.port.in.PatientEvolutionUseCase;
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

/** Evoluções clínicas do paciente (ficha do paciente, Fase 2). */
@RestController
public class PatientEvolutionController {

    private static final String ACCESS_MESSAGE =
            "Você só pode acessar evoluções da sua própria empresa.";

    private final PatientEvolutionUseCase useCase;

    public PatientEvolutionController(PatientEvolutionUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/api/v1/companies/{companyId}/patients/{contactId}/evolutions")
    @PreAuthorize("hasAuthority('contact:read')")
    public ResponseEntity<List<PatientEvolutionResponse>> list(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @PathVariable UUID contactId) {
        return ResponseEntity.ok(useCase.list(companyId, contactId));
    }

    @PostMapping("/api/v1/companies/{companyId}/patients/{contactId}/evolutions")
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<PatientEvolutionResponse> create(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @PathVariable UUID contactId,
            @Valid @RequestBody PatientEvolutionRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(useCase.create(companyId, contactId, request, principal.userId()));
    }

    @PutMapping("/api/v1/companies/{companyId}/patients/{contactId}/evolutions/{evolutionId}")
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<PatientEvolutionResponse> update(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @PathVariable UUID contactId,
            @PathVariable UUID evolutionId,
            @Valid @RequestBody PatientEvolutionRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(useCase.update(companyId, contactId, evolutionId, request,
                principal.userId(), isAdmin(principal)));
    }

    @PostMapping("/api/v1/companies/{companyId}/patients/{contactId}/evolutions/{evolutionId}/finalize")
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<PatientEvolutionResponse> finalizeEvolution(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @PathVariable UUID contactId,
            @PathVariable UUID evolutionId,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(useCase.finalizeEvolution(companyId, contactId, evolutionId,
                principal.userId()));
    }

    @DeleteMapping("/api/v1/companies/{companyId}/patients/{contactId}/evolutions/{evolutionId}")
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<Void> delete(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @PathVariable UUID contactId,
            @PathVariable UUID evolutionId,
            @AuthenticationPrincipal CurrentUser principal) {
        useCase.delete(companyId, contactId, evolutionId, principal.userId(), isAdmin(principal));
        return ResponseEntity.noContent().build();
    }

    private static boolean isAdmin(CurrentUser principal) {
        return Membership.isAdminLevelRole(principal.membershipRole())
                || principal.roles().stream().anyMatch(Membership::isAdminLevelRole);
    }
}
