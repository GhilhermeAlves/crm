package com.becommerce.crm.masterdata.contact.note.web;

import com.becommerce.crm.identity.membership.domain.Membership;
import com.becommerce.crm.masterdata.contact.note.application.dto.ContactNoteRequest;
import com.becommerce.crm.masterdata.contact.note.application.dto.ContactNoteResponse;
import com.becommerce.crm.masterdata.contact.note.application.port.in.ContactNoteUseCase;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Anotações do paciente (ficha do paciente, Fase 1). Scoped à empresa ativa via
 * {@link CurrentCompanyId}; edição/exclusão permitida ao autor ou a
 * administradores (ADMIN/OWNER/SUPER_ADMIN).
 */
@RestController
public class ContactNoteController {

    private final ContactNoteUseCase useCase;

    public ContactNoteController(ContactNoteUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/api/v1/companies/{companyId}/contacts/{contactId}/notes")
    @PreAuthorize("hasAuthority('contact:read')")
    public ResponseEntity<List<ContactNoteResponse>> list(
            @CurrentCompanyId("Você só pode acessar anotações da sua própria empresa.") UUID companyId,
            @PathVariable UUID contactId) {
        return ResponseEntity.ok(useCase.list(companyId, contactId));
    }

    @PostMapping("/api/v1/companies/{companyId}/contacts/{contactId}/notes")
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<ContactNoteResponse> create(
            @CurrentCompanyId("Você só pode acessar anotações da sua própria empresa.") UUID companyId,
            @PathVariable UUID contactId,
            @Valid @RequestBody ContactNoteRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(useCase.create(companyId, contactId, request, principal.userId()));
    }

    @PutMapping("/api/v1/companies/{companyId}/contacts/{contactId}/notes/{noteId}")
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<ContactNoteResponse> update(
            @CurrentCompanyId("Você só pode acessar anotações da sua própria empresa.") UUID companyId,
            @PathVariable UUID contactId,
            @PathVariable UUID noteId,
            @Valid @RequestBody ContactNoteRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(useCase.update(companyId, contactId, noteId, request,
                principal.userId(), isAdmin(principal)));
    }

    @DeleteMapping("/api/v1/companies/{companyId}/contacts/{contactId}/notes/{noteId}")
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<Void> delete(
            @CurrentCompanyId("Você só pode acessar anotações da sua própria empresa.") UUID companyId,
            @PathVariable UUID contactId,
            @PathVariable UUID noteId,
            @AuthenticationPrincipal CurrentUser principal) {
        useCase.delete(companyId, contactId, noteId, principal.userId(), isAdmin(principal));
        return ResponseEntity.noContent().build();
    }

    private static boolean isAdmin(CurrentUser principal) {
        return Membership.isAdminLevelRole(principal.membershipRole())
                || principal.roles().stream().anyMatch(Membership::isAdminLevelRole);
    }
}
