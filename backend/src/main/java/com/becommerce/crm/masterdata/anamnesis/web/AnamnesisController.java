package com.becommerce.crm.masterdata.anamnesis.web;

import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisModelResponse;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisModelSummaryResponse;
import com.becommerce.crm.masterdata.anamnesis.application.dto.CreateAnamnesisModelRequest;
import com.becommerce.crm.masterdata.anamnesis.application.dto.UpdateAnamnesisModelRequest;
import com.becommerce.crm.masterdata.anamnesis.application.service.AnamnesisService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Modelos de anamnese (V090). Isolamento por RLS FORCE + {@code @CurrentCompanyId}.
 * A listagem provisiona automaticamente o modelo padrão odontológico da empresa na
 * primeira consulta.
 */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/anamnesis-models")
public class AnamnesisController {

    private static final String OWN_COMPANY = "Você só pode acessar as anamneses da sua própria empresa.";

    private final AnamnesisService anamnesisService;

    public AnamnesisController(AnamnesisService anamnesisService) {
        this.anamnesisService = anamnesisService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('anamnesis:read')")
    public ResponseEntity<List<AnamnesisModelSummaryResponse>> list(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(anamnesisService.list(companyId, principal.userId()));
    }

    @GetMapping("/{modelId}")
    @PreAuthorize("hasAuthority('anamnesis:read')")
    public ResponseEntity<AnamnesisModelResponse> getById(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                                          @PathVariable UUID modelId) {
        return ResponseEntity.ok(anamnesisService.getById(companyId, modelId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('anamnesis:manage')")
    public ResponseEntity<AnamnesisModelResponse> create(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                                         @Valid @RequestBody CreateAnamnesisModelRequest request,
                                                         @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(anamnesisService.create(companyId, request, principal.userId()));
    }

    @PutMapping("/{modelId}")
    @PreAuthorize("hasAuthority('anamnesis:manage')")
    public ResponseEntity<AnamnesisModelResponse> update(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                                         @PathVariable UUID modelId,
                                                         @Valid @RequestBody UpdateAnamnesisModelRequest request,
                                                         @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(anamnesisService.update(companyId, modelId, request, principal.userId()));
    }

    @PostMapping("/{modelId}/activate")
    @PreAuthorize("hasAuthority('anamnesis:manage')")
    public ResponseEntity<AnamnesisModelResponse> activate(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                                           @PathVariable UUID modelId,
                                                           @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(anamnesisService.setActive(companyId, modelId, true, principal.userId()));
    }

    @PostMapping("/{modelId}/deactivate")
    @PreAuthorize("hasAuthority('anamnesis:manage')")
    public ResponseEntity<AnamnesisModelResponse> deactivate(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                                             @PathVariable UUID modelId,
                                                             @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(anamnesisService.setActive(companyId, modelId, false, principal.userId()));
    }

    @DeleteMapping("/{modelId}")
    @PreAuthorize("hasAuthority('anamnesis:manage')")
    public ResponseEntity<Void> delete(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                       @PathVariable UUID modelId,
                                       @AuthenticationPrincipal CurrentUser principal) {
        anamnesisService.delete(companyId, modelId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
