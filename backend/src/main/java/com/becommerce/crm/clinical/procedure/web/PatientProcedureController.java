package com.becommerce.crm.clinical.procedure.web;

import com.becommerce.crm.clinical.procedure.application.dto.CompleteProcedureRequest;
import com.becommerce.crm.clinical.procedure.application.dto.PatientProcedureRequest;
import com.becommerce.crm.clinical.procedure.application.dto.PatientProcedureResponse;
import com.becommerce.crm.clinical.procedure.application.port.in.PatientProcedureUseCase;
import com.becommerce.crm.clinical.procedure.domain.ProcedureState;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Procedimentos da ficha clínica do paciente (Fase 3a). */
@RestController
public class PatientProcedureController {

    private static final String ACCESS_MESSAGE =
            "Você só pode acessar procedimentos da sua própria empresa.";
    private static final String BASE = "/api/v1/companies/{companyId}/patients/{contactId}/procedures";

    private final PatientProcedureUseCase useCase;

    public PatientProcedureController(PatientProcedureUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping(BASE)
    @PreAuthorize("hasAuthority('contact:read')")
    public ResponseEntity<List<PatientProcedureResponse>> list(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @PathVariable UUID contactId,
            @RequestParam(required = false) ProcedureState state,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer tooth) {
        return ResponseEntity.ok(useCase.list(companyId, contactId, state, category, tooth));
    }

    @PostMapping(BASE)
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<PatientProcedureResponse> create(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @PathVariable UUID contactId,
            @Valid @RequestBody PatientProcedureRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                useCase.create(companyId, contactId, request, principal.userId(), canCreateFinance(principal)));
    }

    @PatchMapping(BASE + "/{procedureId}")
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<PatientProcedureResponse> update(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @PathVariable UUID contactId,
            @PathVariable UUID procedureId,
            @Valid @RequestBody PatientProcedureRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(useCase.update(companyId, contactId, procedureId, request, principal.userId()));
    }

    @PostMapping(BASE + "/{procedureId}/complete")
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<PatientProcedureResponse> complete(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @PathVariable UUID contactId,
            @PathVariable UUID procedureId,
            @Valid @RequestBody CompleteProcedureRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(useCase.complete(companyId, contactId, procedureId, request,
                principal.userId(), canCreateFinance(principal)));
    }

    @DeleteMapping(BASE + "/{procedureId}")
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<Void> delete(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @PathVariable UUID contactId,
            @PathVariable UUID procedureId,
            @AuthenticationPrincipal CurrentUser principal) {
        useCase.delete(companyId, contactId, procedureId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    private static boolean canCreateFinance(CurrentUser p) {
        return p.permissions() != null && p.permissions().contains("finance:create");
    }
}
