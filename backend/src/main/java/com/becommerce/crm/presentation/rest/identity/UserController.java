package com.becommerce.crm.presentation.rest.identity;

import com.becommerce.crm.application.identity.dto.*;
import com.becommerce.crm.application.identity.port.input.RoleUseCase;
import com.becommerce.crm.application.identity.port.input.UserUseCase;
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
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserUseCase userUseCase;
    private final RoleUseCase roleUseCase;

    public UserController(UserUseCase userUseCase, RoleUseCase roleUseCase) {
        this.userUseCase = userUseCase;
        this.roleUseCase = roleUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<PageResponse<UserResponse>> listUsers(
            @AuthenticationPrincipal CurrentUser principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        return ResponseEntity.ok(userUseCase.listUsers(
                principal.companyId(), search, status, page, pageSize, sortBy, sortDirection));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userUseCase.getUserById(id));
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userUseCase.getUserByEmail(email));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user:create')")
    public ResponseEntity<UserResponse> createUser(
            @AuthenticationPrincipal CurrentUser principal,
            @Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userUseCase.createUser(principal.companyId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:update')")
    public ResponseEntity<UserResponse> updateUser(@PathVariable UUID id,
                                                   @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userUseCase.updateUser(id, request));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('user:update')")
    public ResponseEntity<Void> activateUser(@PathVariable UUID id) {
        userUseCase.activateUser(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('user:update')")
    public ResponseEntity<Void> deactivateUser(@PathVariable UUID id) {
        userUseCase.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:delete')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userUseCase.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invite")
    @PreAuthorize("hasAuthority('user:invite')")
    public ResponseEntity<UserResponse> inviteUser(
            @AuthenticationPrincipal CurrentUser principal,
            @Valid @RequestBody InviteUserRequest request) {
        UserResponse response = userUseCase.inviteUser(principal.companyId(), principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/accept-invite")
    public ResponseEntity<UserResponse> acceptInvite(@RequestBody AcceptInviteRequest request) {
        UserResponse response = userUseCase.acceptInvite(request.token(), request.password());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(userUseCase.getProfile(principal.userId()));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal CurrentUser principal,
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userUseCase.updateProfile(principal.userId(), request));
    }

    @GetMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('role:read')")
    public ResponseEntity<List<RoleResponse>> getUserRoles(
            @PathVariable UUID id,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(roleUseCase.getUserRoles(id, principal.companyId()));
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('role:assign')")
    public ResponseEntity<Void> assignRoleToUser(
            @PathVariable UUID id,
            @Valid @RequestBody AssignRoleRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        roleUseCase.assignRoleToUser(id, UUID.fromString(request.roleId()), principal.companyId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    @PreAuthorize("hasAuthority('role:assign')")
    public ResponseEntity<Void> removeRoleFromUser(
            @PathVariable UUID id,
            @PathVariable UUID roleId) {
        roleUseCase.removeRoleFromUser(id, roleId);
        return ResponseEntity.noContent().build();
    }
}
