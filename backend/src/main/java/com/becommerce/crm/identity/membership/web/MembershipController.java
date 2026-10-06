package com.becommerce.crm.identity.membership.web;

import com.becommerce.crm.identity.membership.application.dto.MemberResponse;
import com.becommerce.crm.identity.membership.application.dto.MembershipResponse;
import com.becommerce.crm.identity.membership.application.dto.UpdateMemberRoleRequest;
import com.becommerce.crm.identity.membership.application.port.input.MembershipUseCase;
import com.becommerce.crm.identity.membership.domain.MembershipStatus;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class MembershipController {

    private final MembershipUseCase membershipUseCase;

    public MembershipController(MembershipUseCase membershipUseCase) {
        this.membershipUseCase = membershipUseCase;
    }

    @GetMapping("/companies/{id}/members")
    @PreAuthorize("hasAuthority('membership:view')")
    public ResponseEntity<List<MemberResponse>> listMembers(
            @PathVariable UUID id,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(
                membershipUseCase.listMembers(id, principal.companyId(), parseStatus(status)));
    }

    @PutMapping("/companies/{id}/members/{userId}")
    @PreAuthorize("hasAuthority('membership:manage')")
    public ResponseEntity<MemberResponse> updateMemberRole(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        MemberResponse response = membershipUseCase.updateMemberRole(
                id, userId, request.role(), principal.companyId(), isSuperAdmin(principal));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/companies/{id}/members/{userId}")
    @PreAuthorize("hasAuthority('membership:manage')")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @AuthenticationPrincipal CurrentUser principal) {
        membershipUseCase.removeMember(id, userId, principal.companyId(), isSuperAdmin(principal));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/memberships")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MembershipResponse>> myMemberships(
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(membershipUseCase.listMyMemberships(principal.userId()));
    }

    private boolean isSuperAdmin(CurrentUser principal) {
        return principal.roles().contains("SUPER_ADMIN");
    }

    /**
     * Filtro opcional de status da membership. Vazio/ausente = histórico
     * ({@code ACTIVE}); valor desconhecido = 400 (o GlobalExceptionHandler
     * responde 400 para IllegalStateException, com mensagem genérica — lacuna
     * já documentada).
     */
    private static MembershipStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return MembershipStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Status de membro inválido: " + status);
        }
    }
}
