package com.becommerce.crm.presentation.rest.customer360;

import com.becommerce.crm.application.customer360.dto.Customer360Response;
import com.becommerce.crm.application.customer360.service.Customer360Service;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Customer 360 (Sprint 13). Consolida dados do contato, contexto comercial,
 * tarefas, linha do tempo e próxima ação. Exige {@code contact:read} e é
 * scoped à empresa ativa.
 */
@RestController
public class Customer360Controller {

    private final Customer360Service customer360Service;

    public Customer360Controller(Customer360Service customer360Service) {
        this.customer360Service = customer360Service;
    }

    @GetMapping("/api/v1/companies/{companyId}/contacts/{contactId}/360")
    @PreAuthorize("hasAuthority('contact:read')")
    public ResponseEntity<Customer360Response> customer360(
            @PathVariable UUID companyId,
            @PathVariable UUID contactId,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(customer360Service.build(companyId, contactId));
    }

    private void requireCompanyAccess(UUID companyId, CurrentUser principal) {
        boolean superAdmin = principal.roles().contains("SUPER_ADMIN");
        if (!superAdmin && !companyId.equals(principal.companyId())) {
            throw new CrmAccessDeniedException("Você só pode acessar contatos da sua própria empresa.");
        }
    }
}
