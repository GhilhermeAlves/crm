package com.becommerce.crm.clinical.documenttemplate.web;

import com.becommerce.crm.clinical.documenttemplate.application.dto.DocumentTemplateRequest;
import com.becommerce.crm.clinical.documenttemplate.application.dto.DocumentTemplateResponse;
import com.becommerce.crm.clinical.documenttemplate.application.port.in.DocumentTemplateUseCase;
import com.becommerce.crm.clinical.documenttemplate.domain.DocumentType;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Modelos de documento da empresa (Configurações -> Documentos, Fase 2 da
 * ficha). Leitura exige {@code anamnesis:read} e escrita {@code anamnesis:manage}
 * (mesma área de configurações clínicas).
 */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/document-templates")
public class DocumentTemplateController {

    private static final String OWN_COMPANY =
            "Você só pode acessar modelos de documento da sua própria empresa.";

    private final DocumentTemplateUseCase useCase;

    public DocumentTemplateController(DocumentTemplateUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('anamnesis:read')")
    public ResponseEntity<List<DocumentTemplateResponse>> list(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @RequestParam(required = false) DocumentType type,
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(useCase.list(companyId, type, activeOnly));
    }

    @GetMapping("/{templateId}")
    @PreAuthorize("hasAuthority('anamnesis:read')")
    public ResponseEntity<DocumentTemplateResponse> get(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @PathVariable UUID templateId) {
        return ResponseEntity.ok(useCase.get(companyId, templateId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('anamnesis:manage')")
    public ResponseEntity<DocumentTemplateResponse> create(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @Valid @RequestBody DocumentTemplateRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(useCase.create(companyId, request, principal.userId()));
    }

    @PutMapping("/{templateId}")
    @PreAuthorize("hasAuthority('anamnesis:manage')")
    public ResponseEntity<DocumentTemplateResponse> update(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @PathVariable UUID templateId,
            @Valid @RequestBody DocumentTemplateRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(useCase.update(companyId, templateId, request, principal.userId()));
    }

    @DeleteMapping("/{templateId}")
    @PreAuthorize("hasAuthority('anamnesis:manage')")
    public ResponseEntity<Void> delete(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @PathVariable UUID templateId,
            @AuthenticationPrincipal CurrentUser principal) {
        useCase.delete(companyId, templateId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
