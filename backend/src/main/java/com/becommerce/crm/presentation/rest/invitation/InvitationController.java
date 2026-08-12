package com.becommerce.crm.presentation.rest.invitation;

import com.becommerce.crm.application.invitation.dto.CreateInvitationRequest;
import com.becommerce.crm.application.invitation.dto.InvitationResponse;
import com.becommerce.crm.application.invitation.port.input.InvitationUseCase;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.crm.domain.invitation.InvitationStatus;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Convites (Sprint 8.5). Acesso administrativo exige que o solicitante esteja
 * na própria empresa (ADMIN/OWNER, via permission membership:manage). Aceite e
 * recusa são token-based (qualquer usuário autenticado com o token correto).
 */
@RestController
@RequestMapping("/api/v1")
public class InvitationController {

    private final InvitationUseCase invitationUseCase;

    public InvitationController(InvitationUseCase invitationUseCase) {
        this.invitationUseCase = invitationUseCase;
    }

    @PostMapping("/companies/{companyId}/invitations")
    @PreAuthorize("hasAuthority('membership:manage')")
    public ResponseEntity<InvitationResponse> create(@PathVariable UUID companyId,
                                                     @Valid @RequestBody CreateInvitationRequest request,
                                                     @AuthenticationPrincipal CurrentUser principal) {
        requireAdminAccess(companyId, principal);
        InvitationResponse response = invitationUseCase.create(companyId, request, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/companies/{companyId}/invitations")
    @PreAuthorize("hasAuthority('membership:view')")
    public ResponseEntity<List<InvitationResponse>> list(@PathVariable UUID companyId,
                                                         @RequestParam(required = false) String status,
                                                         @AuthenticationPrincipal CurrentUser principal) {
        requireAdminAccess(companyId, principal);
        InvitationStatus filter = status == null || status.isBlank()
                ? null : InvitationStatus.valueOf(status.toUpperCase());
        return ResponseEntity.ok(invitationUseCase.listByCompany(companyId, filter));
    }

    @DeleteMapping("/companies/{companyId}/invitations/{invitationId}")
    @PreAuthorize("hasAuthority('membership:manage')")
    public ResponseEntity<Void> revoke(@PathVariable UUID companyId,
                                       @PathVariable UUID invitationId,
                                       @AuthenticationPrincipal CurrentUser principal) {
        requireAdminAccess(companyId, principal);
        invitationUseCase.revoke(invitationId, companyId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invitations/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InvitationResponse> accept(@RequestParam String token,
                                                     @AuthenticationPrincipal CurrentUser principal) {
        InvitationResponse response = invitationUseCase.accept(token, principal.userId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/invitations/decline")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InvitationResponse> decline(@RequestParam String token,
                                                      @AuthenticationPrincipal CurrentUser principal) {
        InvitationResponse response = invitationUseCase.decline(token, principal.userId());
        return ResponseEntity.ok(response);
    }

    private void requireAdminAccess(UUID companyId, CurrentUser principal) {
        boolean superAdmin = principal.roles().contains("SUPER_ADMIN");
        if (!superAdmin && !companyId.equals(principal.companyId())) {
            throw new CrmAccessDeniedException("Você só pode administrar convites da sua própria empresa.");
        }
    }
}