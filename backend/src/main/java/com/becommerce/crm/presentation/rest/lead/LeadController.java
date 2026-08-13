package com.becommerce.crm.presentation.rest.lead;

import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.application.lead.dto.CreateLeadRequest;
import com.becommerce.crm.application.lead.dto.LeadResponse;
import com.becommerce.crm.application.lead.dto.UpdateLeadRequest;
import com.becommerce.crm.application.lead.port.input.LeadUseCase;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Leads (Sprint 10). Acesso restrito à própria empresa; RLS FORCE (V021) e
 * verificação {@code requireCompanyAccess} no controller + {@code TenantContext}
 * no serviço garantem isolamento multi-tenant real.
 */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/leads")
public class LeadController {

    private final LeadUseCase leadUseCase;

    public LeadController(LeadUseCase leadUseCase) {
        this.leadUseCase = leadUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('lead:read')")
    public ResponseEntity<PageResponse<LeadResponse>> list(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal CurrentUser principal,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String classification,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(leadUseCase.list(
                companyId, status, source, classification, page, pageSize, sortBy, sortDirection));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('lead:create')")
    public ResponseEntity<LeadResponse> create(@PathVariable UUID companyId,
                                               @Valid @RequestBody CreateLeadRequest request,
                                               @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        LeadResponse response = leadUseCase.create(companyId, request, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{leadId}")
    @PreAuthorize("hasAuthority('lead:read')")
    public ResponseEntity<LeadResponse> getById(@PathVariable UUID companyId,
                                                @PathVariable UUID leadId,
                                                @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(leadUseCase.getById(companyId, leadId));
    }

    @PutMapping("/{leadId}")
    @PreAuthorize("hasAuthority('lead:update')")
    public ResponseEntity<LeadResponse> update(@PathVariable UUID companyId,
                                               @PathVariable UUID leadId,
                                               @Valid @RequestBody UpdateLeadRequest request,
                                               @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(leadUseCase.update(companyId, leadId, request));
    }

    @DeleteMapping("/{leadId}")
    @PreAuthorize("hasAuthority('lead:delete')")
    public ResponseEntity<Void> delete(@PathVariable UUID companyId,
                                       @PathVariable UUID leadId,
                                       @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        leadUseCase.delete(companyId, leadId);
        return ResponseEntity.noContent().build();
    }

    private void requireCompanyAccess(UUID companyId, CurrentUser principal) {
        boolean superAdmin = principal.roles().contains("SUPER_ADMIN");
        if (!superAdmin && !companyId.equals(principal.companyId())) {
            throw new CrmAccessDeniedException("Você só pode acessar leads da sua própria empresa.");
        }
    }
}