package com.becommerce.crm.presentation.rest.identity;

import com.becommerce.crm.application.identity.dto.*;
import com.becommerce.crm.application.identity.port.input.RoleUseCase;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleUseCase roleUseCase;

    public RoleController(RoleUseCase roleUseCase) {
        this.roleUseCase = roleUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('role:read')")
    public ResponseEntity<List<RoleResponse>> listRoles(
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(roleUseCase.listRoles(principal.companyId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('role:read')")
    public ResponseEntity<RoleResponse> getRoleById(
            @PathVariable UUID id,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(id, principal);
        return ResponseEntity.ok(roleUseCase.getRoleById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('role:create')")
    public ResponseEntity<RoleResponse> createRole(
            @AuthenticationPrincipal CurrentUser principal,
            @Valid @RequestBody CreateRoleRequest request) {
        RoleResponse response = roleUseCase.createRole(principal.companyId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('role:update')")
    public ResponseEntity<RoleResponse> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(id, principal);
        return ResponseEntity.ok(roleUseCase.updateRole(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role:delete')")
    public ResponseEntity<Void> deleteRole(
            @PathVariable UUID id,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(id, principal);
        roleUseCase.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{roleId}/permissions")
    @PreAuthorize("hasAuthority('permission:assign')")
    public ResponseEntity<Void> assignPermission(
            @PathVariable UUID roleId,
            @Valid @RequestBody AssignPermissionRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(roleId, principal);
        roleUseCase.assignPermissionToRole(roleId, request.permissionId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('permission:assign')")
    public ResponseEntity<Void> removePermission(
            @PathVariable UUID roleId,
            @PathVariable UUID permissionId,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(roleId, principal);
        roleUseCase.removePermissionFromRole(roleId, permissionId.toString());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('role:assign')")
    public ResponseEntity<Void> assignRoleToUser(
            @PathVariable UUID userId,
            @Valid @RequestBody AssignRoleRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        roleUseCase.assignRoleToUser(userId, UUID.fromString(request.roleId()), principal.companyId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/user/{userId}/roles/{roleId}")
    @PreAuthorize("hasAuthority('role:assign')")
    public ResponseEntity<Void> removeRoleFromUser(
            @PathVariable UUID userId,
            @PathVariable UUID roleId) {
        roleUseCase.removeRoleFromUser(userId, roleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('role:read')")
    public ResponseEntity<List<RoleResponse>> getUserRoles(
            @PathVariable UUID userId,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(roleUseCase.getUserRoles(userId, principal.companyId()));
    }

    private void requireCompanyAccess(UUID roleId, CurrentUser principal) {
        boolean superAdmin = principal.roles().contains("SUPER_ADMIN");
        if (superAdmin) {
            return;
        }
        RoleResponse role = roleUseCase.getRoleById(roleId);
        if (role.companyId() == null || !UUID.fromString(role.companyId()).equals(principal.companyId())) {
            throw new CrmAccessDeniedException("Você só pode acessar roles da sua própria empresa.");
        }
    }
}
