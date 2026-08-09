package com.becommerce.crm.presentation.rest.membership;

import com.becommerce.crm.application.membership.dto.MemberResponse;
import com.becommerce.crm.application.membership.dto.MembershipResponse;
import com.becommerce.crm.application.membership.dto.UpdateMemberRoleRequest;
import com.becommerce.crm.application.membership.port.input.MembershipUseCase;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(membershipUseCase.listMembers(id, principal.companyId()));
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
}
