package com.becommerce.crm.presentation.rest.contact;

import com.becommerce.crm.application.contact.dto.ContactResponse;
import com.becommerce.crm.application.contact.dto.CreateContactRequest;
import com.becommerce.crm.application.contact.port.input.ContactUseCase;
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
    public ResponseEntity<ContactResponse> create(@PathVariable UUID companyId,
                                                  @Valid @RequestBody CreateContactRequest request,
                                                  @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        ContactResponse response = contactUseCase.create(companyId, request, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{contactId}")
    @PreAuthorize("hasAuthority('contact:read')")
    public ResponseEntity<ContactResponse> getById(@PathVariable UUID companyId,
                                                   @PathVariable UUID contactId,
                                                   @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(contactUseCase.getById(companyId, contactId));
    }

    private void requireCompanyAccess(UUID companyId, CurrentUser principal) {
        boolean superAdmin = principal.roles().contains("SUPER_ADMIN");
        if (!superAdmin && !companyId.equals(principal.companyId())) {
            throw new CrmAccessDeniedException("Você só pode acessar contatos da sua própria empresa.");
        }
    }
}