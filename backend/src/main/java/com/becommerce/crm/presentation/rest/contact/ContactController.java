package com.becommerce.crm.presentation.rest.contact;

import com.becommerce.crm.application.contact.dto.ContactResponse;
import com.becommerce.crm.application.contact.dto.CreateContactRequest;
import com.becommerce.crm.application.contact.dto.UpdateContactRequest;
import com.becommerce.crm.application.contact.port.input.ContactUseCase;
import com.becommerce.crm.infrastructure.security.config.CurrentCompanyId;
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
 * Contatos (Sprint 8.6). Acesso restrito à própria empresa; o limite de
 * contatos do plano é aplicado pela camada de serviço (Code 422 QUOTA_EXCEEDED).
 */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/contacts")
public class ContactController {

    private final ContactUseCase contactUseCase;

    public ContactController(ContactUseCase contactUseCase) {
        this.contactUseCase = contactUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('contact:create')")
    public ResponseEntity<ContactResponse> create(
            @CurrentCompanyId("Você só pode acessar contatos da sua própria empresa.") UUID companyId,
            @Valid @RequestBody CreateContactRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        ContactResponse response = contactUseCase.create(companyId, request, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{contactId}")
    @PreAuthorize("hasAuthority('contact:read')")
    public ResponseEntity<ContactResponse> getById(
            @CurrentCompanyId("Você só pode acessar contatos da sua própria empresa.") UUID companyId,
            @PathVariable UUID contactId) {
        return ResponseEntity.ok(contactUseCase.getById(companyId, contactId));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('contact:read')")
    public ResponseEntity<List<ContactResponse>> list(
            @CurrentCompanyId("Você só pode acessar contatos da sua própria empresa.") UUID companyId) {
        return ResponseEntity.ok(contactUseCase.listByCompany(companyId));
    }

    @PutMapping("/{contactId}")
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<ContactResponse> update(
            @CurrentCompanyId("Você só pode acessar contatos da sua própria empresa.") UUID companyId,
            @PathVariable UUID contactId,
            @Valid @RequestBody UpdateContactRequest request) {
        return ResponseEntity.ok(contactUseCase.update(companyId, contactId, request));
    }

    @DeleteMapping("/{contactId}")
    @PreAuthorize("hasAuthority('contact:delete')")
    public ResponseEntity<Void> delete(
            @CurrentCompanyId("Você só pode acessar contatos da sua própria empresa.") UUID companyId,
            @PathVariable UUID contactId) {
        contactUseCase.delete(companyId, contactId);
        return ResponseEntity.noContent().build();
    }
}