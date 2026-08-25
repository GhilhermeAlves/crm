package com.becommerce.crm.presentation.rest.campaign;

import com.becommerce.crm.application.campaign.dto.AttachChannelRequest;
import com.becommerce.crm.application.campaign.dto.CampaignResponse;
import com.becommerce.crm.application.campaign.dto.CreateCampaignRequest;
import com.becommerce.crm.application.campaign.dto.ExecutionResponse;
import com.becommerce.crm.application.campaign.dto.ScheduleCampaignRequest;
import com.becommerce.crm.application.campaign.dto.UpdateCampaignRequest;
import com.becommerce.crm.application.campaign.port.input.CampaignUseCase;
import com.becommerce.crm.application.identity.dto.PageResponse;
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
 * Campanhas (Sprint 17). Acesso restrito à própria empresa; RLS FORCE (V056)
 * + requireCompanyAccess + TenantContext garantem isolamento multi-tenant.
 */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/campaigns")
public class CampaignController {

    private final CampaignUseCase campaignUseCase;

    public CampaignController(CampaignUseCase campaignUseCase) {
        this.campaignUseCase = campaignUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('campaign:read')")
    public ResponseEntity<PageResponse<CampaignResponse>> list(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal CurrentUser principal,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String audienceType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(campaignUseCase.list(companyId, status, audienceType, page, pageSize));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('campaign:create')")
    public ResponseEntity<CampaignResponse> create(@PathVariable UUID companyId,
                                                   @Valid @RequestBody CreateCampaignRequest request,
                                                   @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        CampaignResponse response = campaignUseCase.create(companyId, request, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{campaignId}")
    @PreAuthorize("hasAuthority('campaign:read')")
    public ResponseEntity<CampaignResponse> getById(@PathVariable UUID companyId,
                                                    @PathVariable UUID campaignId,
                                                    @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(campaignUseCase.getById(companyId, campaignId));
    }

    @PutMapping("/{campaignId}")
    @PreAuthorize("hasAuthority('campaign:update')")
    public ResponseEntity<CampaignResponse> update(@PathVariable UUID companyId,
                                                   @PathVariable UUID campaignId,
                                                   @Valid @RequestBody UpdateCampaignRequest request,
                                                   @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(campaignUseCase.update(companyId, campaignId, request));
    }

    @DeleteMapping("/{campaignId}")
    @PreAuthorize("hasAuthority('campaign:delete')")
    public ResponseEntity<Void> delete(@PathVariable UUID companyId,
                                       @PathVariable UUID campaignId,
                                       @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        campaignUseCase.delete(companyId, campaignId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{campaignId}/channel")
    @PreAuthorize("hasAuthority('campaign:update')")
    public ResponseEntity<CampaignResponse> attachChannel(@PathVariable UUID companyId,
                                                          @PathVariable UUID campaignId,
                                                          @Valid @RequestBody AttachChannelRequest request,
                                                          @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(campaignUseCase.attachChannel(
                companyId, campaignId, request, principal.userId()));
    }

    @PostMapping("/{campaignId}/schedule")
    @PreAuthorize("hasAuthority('campaign:execute')")
    public ResponseEntity<CampaignResponse> schedule(@PathVariable UUID companyId,
                                                     @PathVariable UUID campaignId,
                                                     @Valid @RequestBody ScheduleCampaignRequest request,
                                                     @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(campaignUseCase.schedule(
                companyId, campaignId, request, principal.userId()));
    }

    @PostMapping("/{campaignId}/execute")
    @PreAuthorize("hasAuthority('campaign:execute')")
    public ResponseEntity<ExecutionResponse> executeNow(@PathVariable UUID companyId,
                                                        @PathVariable UUID campaignId,
                                                        @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(campaignUseCase.executeNow(companyId, campaignId, principal.userId()));
    }

    @PostMapping("/{campaignId}/pause")
    @PreAuthorize("hasAuthority('campaign:execute')")
    public ResponseEntity<CampaignResponse> pause(@PathVariable UUID companyId,
                                                  @PathVariable UUID campaignId,
                                                  @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(campaignUseCase.pause(companyId, campaignId, principal.userId()));
    }

    @PostMapping("/{campaignId}/resume")
    @PreAuthorize("hasAuthority('campaign:execute')")
    public ResponseEntity<CampaignResponse> resume(@PathVariable UUID companyId,
                                                   @PathVariable UUID campaignId,
                                                   @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(campaignUseCase.resume(companyId, campaignId, principal.userId()));
    }

    @PostMapping("/{campaignId}/cancel")
    @PreAuthorize("hasAuthority('campaign:execute')")
    public ResponseEntity<CampaignResponse> cancel(@PathVariable UUID companyId,
                                                   @PathVariable UUID campaignId,
                                                   @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(campaignUseCase.cancel(companyId, campaignId, principal.userId()));
    }

    @GetMapping("/{campaignId}/execution")
    @PreAuthorize("hasAuthority('campaign:view_metrics')")
    public ResponseEntity<ExecutionResponse> getExecution(@PathVariable UUID companyId,
                                                          @PathVariable UUID campaignId,
                                                          @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(campaignUseCase.getExecution(companyId, campaignId));
    }

    private void requireCompanyAccess(UUID companyId, CurrentUser principal) {
        boolean superAdmin = principal.roles().contains("SUPER_ADMIN");
        if (!superAdmin && !companyId.equals(principal.companyId())) {
            throw new CrmAccessDeniedException("Você só pode acessar campanhas da sua própria empresa.");
        }
    }
}
