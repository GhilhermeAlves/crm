package com.becommerce.crm.presentation.rest.identity;

import com.becommerce.crm.application.identity.dto.PermissionResponse;
import com.becommerce.crm.application.identity.port.input.PermissionUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionController {

    private final PermissionUseCase permissionUseCase;

    public PermissionController(PermissionUseCase permissionUseCase) {
        this.permissionUseCase = permissionUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('role:read')")
    public ResponseEntity<List<PermissionResponse>> listAllPermissions() {
        return ResponseEntity.ok(permissionUseCase.listAllPermissions());
    }

    @GetMapping("/module/{module}")
    @PreAuthorize("hasAuthority('role:read')")
    public ResponseEntity<List<PermissionResponse>> listPermissionsByModule(@PathVariable String module) {
        return ResponseEntity.ok(permissionUseCase.listPermissionsByModule(module));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('role:read')")
    public ResponseEntity<PermissionResponse> getPermissionById(@PathVariable String id) {
        return ResponseEntity.ok(permissionUseCase.getPermissionById(id));
    }
}
