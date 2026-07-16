package com.becommerce.crm.application.identity.service;

import com.becommerce.crm.application.identity.dto.PermissionResponse;
import com.becommerce.crm.application.identity.port.input.PermissionUseCase;
import com.becommerce.crm.application.identity.port.output.PermissionRepository;
import com.becommerce.crm.domain.identity.Permission;
import com.becommerce.crm.domain.identity.exception.PermissionNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PermissionService implements PermissionUseCase {

    private final PermissionRepository permissionRepository;

    public PermissionService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> listAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissionsByModule(String module) {
        return permissionRepository.findByModule(module).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionResponse getPermissionById(String id) {
        Permission permission = permissionRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new PermissionNotFoundException("Permissão não encontrada com ID: " + id));
        return mapToResponse(permission);
    }

    private PermissionResponse mapToResponse(Permission permission) {
        return new PermissionResponse(
                permission.getId().toString(),
                permission.getName(),
                permission.getDescription(),
                permission.getModule(),
                permission.getResource(),
                permission.getAction(),
                permission.getCreatedAt()
        );
    }
}
