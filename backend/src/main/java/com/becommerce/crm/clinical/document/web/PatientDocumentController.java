package com.becommerce.crm.clinical.document.web;

import com.becommerce.crm.clinical.document.application.dto.IssuePatientDocumentRequest;
import com.becommerce.crm.clinical.document.application.dto.PatientDocumentPdf;
import com.becommerce.crm.clinical.document.application.dto.PatientDocumentResponse;
import com.becommerce.crm.clinical.document.application.dto.PreviewPatientDocumentRequest;
import com.becommerce.crm.clinical.document.application.dto.PreviewPatientDocumentResponse;
import com.becommerce.crm.clinical.document.application.port.in.PatientDocumentUseCase;
import com.becommerce.crm.identity.membership.domain.Membership;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Documentos emitidos para o paciente (ficha do paciente, Fase 2). */
@RestController
public class PatientDocumentController {

    private static final String ACCESS_MESSAGE =
            "Você só pode acessar documentos da sua própria empresa.";

    private final PatientDocumentUseCase useCase;

    public PatientDocumentController(PatientDocumentUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/api/v1/companies/{companyId}/patients/{contactId}/documents")
    @PreAuthorize("hasAuthority('contact:read')")
    public ResponseEntity<List<PatientDocumentResponse>> list(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @PathVariable UUID contactId) {
        return ResponseEntity.ok(useCase.list(companyId, contactId));
    }

    @PostMapping("/api/v1/companies/{companyId}/patients/{contactId}/documents/preview")
    @PreAuthorize("hasAuthority('contact:read')")
    public ResponseEntity<PreviewPatientDocumentResponse> preview(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @PathVariable UUID contactId,
            @Valid @RequestBody PreviewPatientDocumentRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(useCase.preview(companyId, contactId, request, principal.userId()));
    }

    @PostMapping("/api/v1/companies/{companyId}/patients/{contactId}/documents")
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<PatientDocumentResponse> issue(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @PathVariable UUID contactId,
            @Valid @RequestBody IssuePatientDocumentRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(useCase.issue(companyId, contactId, request, principal.userId()));
    }

    @GetMapping("/api/v1/companies/{companyId}/patients/{contactId}/documents/{documentId}/pdf")
    @PreAuthorize("hasAuthority('contact:read')")
    public ResponseEntity<byte[]> pdf(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @PathVariable UUID contactId,
            @PathVariable UUID documentId) {
        PatientDocumentPdf pdf = useCase.pdf(companyId, contactId, documentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + pdf.fileName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf.content());
    }

    @DeleteMapping("/api/v1/companies/{companyId}/patients/{contactId}/documents/{documentId}")
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<Void> delete(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @PathVariable UUID contactId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal CurrentUser principal) {
        useCase.delete(companyId, contactId, documentId, isAdmin(principal), principal.userId());
        return ResponseEntity.noContent().build();
    }

    private static boolean isAdmin(CurrentUser principal) {
        return Membership.isAdminLevelRole(principal.membershipRole())
                || principal.roles().stream().anyMatch(Membership::isAdminLevelRole);
    }
}
