package com.becommerce.crm.presentation.rest.activity;

import com.becommerce.crm.application.activity.dto.ActivityResponse;
import com.becommerce.crm.application.activity.dto.CreateActivityRequest;
import com.becommerce.crm.application.activity.dto.UpdateActivityRequest;
import com.becommerce.crm.application.activity.port.input.ActivityUseCase;
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
 * Activities (Sprint 12) — timeline operacional por empresa, contato ou
 * oportunidade. Criação sem escopo externo requer apenas a empresa; os vínculos
 * são opcionais e validados pelo serviço. Crud scoped à empresa ativa.
 */
@RestController
public class ActivityController {

    private final ActivityUseCase activityUseCase;

    public ActivityController(ActivityUseCase activityUseCase) {
        this.activityUseCase = activityUseCase;
    }

    @PostMapping("/api/v1/companies/{companyId}/activities")
    @PreAuthorize("hasAuthority('activity:create')")
    public ResponseEntity<ActivityResponse> create(
            @PathVariable UUID companyId,
            @Valid @RequestBody CreateActivityRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        ActivityResponse response = activityUseCase.create(companyId, request, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/companies/{companyId}/activities")
    @PreAuthorize("hasAuthority('activity:read')")
    public ResponseEntity<List<ActivityResponse>> list(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(activityUseCase.listByCompany(companyId));
    }

    @GetMapping("/api/v1/companies/{companyId}/activities/recent")
    @PreAuthorize("hasAuthority('activity:read')")
    public ResponseEntity<List<ActivityResponse>> recent(
            @PathVariable UUID companyId,
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(activityUseCase.recent(companyId, Math.min(Math.max(limit, 1), 50)));
    }

    @GetMapping("/api/v1/companies/{companyId}/contacts/{contactId}/activities")
    @PreAuthorize("hasAuthority('activity:read')")
    public ResponseEntity<List<ActivityResponse>> listByContact(
            @PathVariable UUID companyId,
            @PathVariable UUID contactId,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(activityUseCase.listByContact(companyId, contactId));
    }

    @GetMapping("/api/v1/companies/{companyId}/opportunities/{opportunityId}/activities")
    @PreAuthorize("hasAuthority('activity:read')")
    public ResponseEntity<List<ActivityResponse>> listByOpportunity(
            @PathVariable UUID companyId,
            @PathVariable UUID opportunityId,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(activityUseCase.listByOpportunity(companyId, opportunityId));
    }

    @GetMapping("/api/v1/companies/{companyId}/activities/{activityId}")
    @PreAuthorize("hasAuthority('activity:read')")
    public ResponseEntity<ActivityResponse> getById(
            @PathVariable UUID companyId,
            @PathVariable UUID activityId,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(activityUseCase.getById(companyId, activityId));
    }

    @PutMapping("/api/v1/companies/{companyId}/activities/{activityId}")
    @PreAuthorize("hasAuthority('activity:update')")
    public ResponseEntity<ActivityResponse> update(
            @PathVariable UUID companyId,
            @PathVariable UUID activityId,
            @Valid @RequestBody UpdateActivityRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(activityUseCase.update(companyId, activityId, request));
    }

    @DeleteMapping("/api/v1/companies/{companyId}/activities/{activityId}")
    @PreAuthorize("hasAuthority('activity:delete')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID companyId,
            @PathVariable UUID activityId,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        activityUseCase.delete(companyId, activityId);
        return ResponseEntity.noContent().build();
    }

    private void requireCompanyAccess(UUID companyId, CurrentUser principal) {
        boolean superAdmin = principal.roles().contains("SUPER_ADMIN");
        if (!superAdmin && !companyId.equals(principal.companyId())) {
            throw new CrmAccessDeniedException("Você só pode acessar atividades da sua própria empresa.");
        }
    }
}