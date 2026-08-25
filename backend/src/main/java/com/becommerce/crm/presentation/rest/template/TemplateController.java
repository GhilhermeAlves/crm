package com.becommerce.crm.presentation.rest.template;

import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.application.template.dto.CreateTemplateRequest;
import com.becommerce.crm.application.template.dto.TemplateResponse;
import com.becommerce.crm.application.template.dto.UpdateTemplateRequest;
import com.becommerce.crm.application.template.port.input.TemplateUseCase;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Templates de mensagem (Sprint 17). Isolamento por RLS FORCE (V055) + TenantContext. */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/templates")
public class TemplateController {

    private final TemplateUseCase templateUseCase;

    public TemplateController(TemplateUseCase templateUseCase) {
        this.templateUseCase = templateUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('template:read')")
    public ResponseEntity<PageResponse<TemplateResponse>> list(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal CurrentUser principal,
            @RequestParam(required = false) String channelType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(templateUseCase.list(companyId, channelType, status, page, pageSize));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('template:create')")
    public ResponseEntity<TemplateResponse> create(@PathVariable UUID companyId,
                                                   @Valid @RequestBody CreateTemplateRequest request,
                                                   @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        TemplateResponse response = templateUseCase.create(companyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{templateId}")
    @PreAuthorize("hasAuthority('template:read')")
    public ResponseEntity<TemplateResponse> getById(@PathVariable UUID companyId,
                                                    @PathVariable UUID templateId,
                                                    @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(templateUseCase.getById(companyId, templateId));
    }

    @PutMapping("/{templateId}")
    @PreAuthorize("hasAuthority('template:update')")
    public ResponseEntity<TemplateResponse> update(@PathVariable UUID companyId,
                                                   @PathVariable UUID templateId,
                                                   @Valid @RequestBody UpdateTemplateRequest request,
                                                   @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(templateUseCase.update(companyId, templateId, request));
    }

    @DeleteMapping("/{templateId}")
    @PreAuthorize("hasAuthority('template:delete')")
    public ResponseEntity<Void> delete(@PathVariable UUID companyId,
                                       @PathVariable UUID templateId,
                                       @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        templateUseCase.delete(companyId, templateId);
        return ResponseEntity.noContent().build();
    }

    private void requireCompanyAccess(UUID companyId, CurrentUser principal) {
        boolean superAdmin = principal.roles().contains("SUPER_ADMIN");
        if (!superAdmin && !companyId.equals(principal.companyId())) {
            throw new CrmAccessDeniedException("Você só pode acessar templates da sua própria empresa.");
        }
    }
}
