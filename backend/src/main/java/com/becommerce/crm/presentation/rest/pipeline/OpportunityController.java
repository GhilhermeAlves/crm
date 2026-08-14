package com.becommerce.crm.presentation.rest.pipeline;

import com.becommerce.crm.application.pipeline.dto.CreateOpportunityRequest;
import com.becommerce.crm.application.pipeline.dto.MarkLostRequest;
import com.becommerce.crm.application.pipeline.dto.MoveOpportunityRequest;
import com.becommerce.crm.application.pipeline.dto.OpportunityHistoryResponse;
import com.becommerce.crm.application.pipeline.dto.OpportunityResponse;
import com.becommerce.crm.application.pipeline.dto.UpdateOpportunityRequest;
import com.becommerce.crm.application.pipeline.port.input.OpportunityUseCase;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Oportunidades (Sprint 11). Criação e listagem são scoped ao pipeline;
 * demais operações referenciam diretamente uma oportunidade da própria empresa.
 */
@RestController
public class OpportunityController {

    private final OpportunityUseCase opportunityUseCase;

    public OpportunityController(OpportunityUseCase opportunityUseCase) {
        this.opportunityUseCase = opportunityUseCase;
    }

    @PostMapping("/api/v1/companies/{companyId}/pipelines/{pipelineId}/opportunities")
    @PreAuthorize("hasAuthority('opportunity:create')")
    public ResponseEntity<OpportunityResponse> create(
            @PathVariable UUID companyId,
            @PathVariable UUID pipelineId,
            @Valid @RequestBody CreateOpportunityRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        OpportunityResponse response = opportunityUseCase.create(companyId, pipelineId, request, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/companies/{companyId}/pipelines/{pipelineId}/opportunities")
    @PreAuthorize("hasAuthority('opportunity:read')")
    public ResponseEntity<List<OpportunityResponse>> listByPipeline(
            @PathVariable UUID companyId,
            @PathVariable UUID pipelineId,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(opportunityUseCase.listByPipeline(companyId, pipelineId));
    }

    @GetMapping("/api/v1/companies/{companyId}/opportunities/{opportunityId}")
    @PreAuthorize("hasAuthority('opportunity:read')")
    public ResponseEntity<OpportunityResponse> getById(
            @PathVariable UUID companyId,
            @PathVariable UUID opportunityId,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(opportunityUseCase.getById(companyId, opportunityId));
    }

    @PutMapping("/api/v1/companies/{companyId}/opportunities/{opportunityId}")
    @PreAuthorize("hasAuthority('opportunity:update')")
    public ResponseEntity<OpportunityResponse> update(
            @PathVariable UUID companyId,
            @PathVariable UUID opportunityId,
            @Valid @RequestBody UpdateOpportunityRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(opportunityUseCase.update(companyId, opportunityId, request, principal.userId()));
    }

    @PostMapping("/api/v1/companies/{companyId}/opportunities/{opportunityId}/move")
    @PreAuthorize("hasAuthority('opportunity:move')")
    public ResponseEntity<OpportunityResponse> move(
            @PathVariable UUID companyId,
            @PathVariable UUID opportunityId,
            @Valid @RequestBody MoveOpportunityRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(opportunityUseCase.move(companyId, opportunityId, request, principal.userId()));
    }

    @PostMapping("/api/v1/companies/{companyId}/opportunities/{opportunityId}/won")
    @PreAuthorize("hasAuthority('opportunity:win')")
    public ResponseEntity<OpportunityResponse> markWon(
            @PathVariable UUID companyId,
            @PathVariable UUID opportunityId,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(opportunityUseCase.markWon(companyId, opportunityId, principal.userId()));
    }

    @PostMapping("/api/v1/companies/{companyId}/opportunities/{opportunityId}/lost")
    @PreAuthorize("hasAuthority('opportunity:lose')")
    public ResponseEntity<OpportunityResponse> markLost(
            @PathVariable UUID companyId,
            @PathVariable UUID opportunityId,
            @Valid @RequestBody MarkLostRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(opportunityUseCase.markLost(companyId, opportunityId, request, principal.userId()));
    }

    @DeleteMapping("/api/v1/companies/{companyId}/opportunities/{opportunityId}")
    @PreAuthorize("hasAuthority('opportunity:delete')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID companyId,
            @PathVariable UUID opportunityId,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        opportunityUseCase.delete(companyId, opportunityId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/companies/{companyId}/opportunities/{opportunityId}/history")
    @PreAuthorize("hasAuthority('opportunity:read')")
    public ResponseEntity<List<OpportunityHistoryResponse>> history(
            @PathVariable UUID companyId,
            @PathVariable UUID opportunityId,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(opportunityUseCase.history(companyId, opportunityId));
    }

    private void requireCompanyAccess(UUID companyId, CurrentUser principal) {
        boolean superAdmin = principal.roles().contains("SUPER_ADMIN");
        if (!superAdmin && !companyId.equals(principal.companyId())) {
            throw new CrmAccessDeniedException("Você só pode acessar oportunidades da sua própria empresa.");
        }
    }
}
