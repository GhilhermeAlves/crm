package com.becommerce.crm.presentation.rest.template;

import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.application.template.dto.CreateTemplateRequest;
import com.becommerce.crm.application.template.dto.TemplateResponse;
import com.becommerce.crm.application.template.dto.UpdateTemplateRequest;
import com.becommerce.crm.application.template.port.input.TemplateUseCase;
import com.becommerce.crm.infrastructure.security.config.CurrentCompanyId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
            @CurrentCompanyId("Você só pode acessar templates da sua própria empresa.") UUID companyId,
            @RequestParam(required = false) String channelType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(templateUseCase.list(companyId, channelType, status, page, pageSize));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('template:create')")
    public ResponseEntity<TemplateResponse> create(
            @CurrentCompanyId("Você só pode acessar templates da sua própria empresa.") UUID companyId,
            @Valid @RequestBody CreateTemplateRequest request) {
        TemplateResponse response = templateUseCase.create(companyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{templateId}")
    @PreAuthorize("hasAuthority('template:read')")
    public ResponseEntity<TemplateResponse> getById(
            @CurrentCompanyId("Você só pode acessar templates da sua própria empresa.") UUID companyId,
            @PathVariable UUID templateId) {
        return ResponseEntity.ok(templateUseCase.getById(companyId, templateId));
    }

    @PutMapping("/{templateId}")
    @PreAuthorize("hasAuthority('template:update')")
    public ResponseEntity<TemplateResponse> update(
            @CurrentCompanyId("Você só pode acessar templates da sua própria empresa.") UUID companyId,
            @PathVariable UUID templateId,
            @Valid @RequestBody UpdateTemplateRequest request) {
        return ResponseEntity.ok(templateUseCase.update(companyId, templateId, request));
    }

    @DeleteMapping("/{templateId}")
    @PreAuthorize("hasAuthority('template:delete')")
    public ResponseEntity<Void> delete(
            @CurrentCompanyId("Você só pode acessar templates da sua própria empresa.") UUID companyId,
            @PathVariable UUID templateId) {
        templateUseCase.delete(companyId, templateId);
        return ResponseEntity.noContent().build();
    }
}