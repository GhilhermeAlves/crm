package com.becommerce.crm.application.identity.service;

import com.becommerce.crm.application.identity.dto.*;
import com.becommerce.crm.application.identity.port.input.RoleUseCase;
import com.becommerce.crm.application.identity.port.output.PermissionRepository;
import com.becommerce.crm.application.identity.port.output.RolePermissionRepository;
import com.becommerce.crm.application.identity.port.output.RoleRepository;
import com.becommerce.crm.application.identity.port.output.UserRoleRepository;
import com.becommerce.crm.domain.identity.Permission;
import com.becommerce.crm.domain.identity.Role;
import com.becommerce.crm.domain.identity.RolePermission;
import com.becommerce.crm.domain.identity.UserRole;
import com.becommerce.crm.domain.identity.exception.DuplicateRoleException;
import com.becommerce.crm.domain.identity.exception.PermissionNotFoundException;
import com.becommerce.crm.domain.identity.exception.RoleNotFoundException;
import com.becommerce.crm.domain.identity.valueobject.RoleName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class RoleService implements RoleUseCase {

    private static final Logger log = LoggerFactory.getLogger(RoleService.class);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;

    public RoleService(RoleRepository roleRepository,
                       PermissionRepository permissionRepository,
                       RolePermissionRepository rolePermissionRepository,
                       UserRoleRepository userRoleRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles(UUID companyId) {
        return roleRepository.findAllByCompanyId(companyId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleById(UUID id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException("Role não encontrada com ID: " + id));
        return mapToResponse(role);
    }

    @Override
    @Transactional
    public RoleResponse createRole(UUID companyId, CreateRoleRequest request) {
        RoleName roleName;
        try {
            roleName = RoleName.valueOf(request.name().toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Nome de role inválido: " + request.name());
        }

        if (roleRepository.existsByNameAndCompanyId(roleName.name(), companyId)) {
            throw new DuplicateRoleException("Já existe uma role com este nome: " + request.name());
        }

        Role role = Role.create(roleName, companyId);
        role.setDescription(request.description());
        Role saved = roleRepository.save(role);

        if (request.permissionIds() != null) {
            for (String permissionId : request.permissionIds()) {
                UUID permId = UUID.fromString(permissionId);
                permissionRepository.findById(permId)
                        .orElseThrow(() -> new PermissionNotFoundException("Permissão não encontrada: " + permissionId));
                RolePermission rp = RolePermission.create(saved.getId(), permId);
                rolePermissionRepository.save(rp);
            }
        }

        log.info("Role criada: {} para empresa {}", saved.getName(), companyId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public RoleResponse updateRole(UUID id, UpdateRoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException("Role não encontrada com ID: " + id));

        if (request.description() != null) {
            role.setDescription(request.description());
        }
        if (request.isActive() != null) {
            role.setActive(request.isActive());
        }
        role.setUpdatedAt(java.time.LocalDateTime.now());
        Role saved = roleRepository.save(role);

        if (request.permissionIds() != null) {
            List<RolePermission> existing = rolePermissionRepository.findByRoleId(id);
            for (RolePermission rp : existing) {
                rolePermissionRepository.deleteByRoleIdAndPermissionId(id, rp.getPermissionId());
            }
            for (String permissionId : request.permissionIds()) {
                UUID permId = UUID.fromString(permissionId);
                permissionRepository.findById(permId)
                        .orElseThrow(() -> new PermissionNotFoundException("Permissão não encontrada: " + permissionId));
                RolePermission rp = RolePermission.create(id, permId);
                rolePermissionRepository.save(rp);
            }
        }

        log.info("Role atualizada: {}", saved.getName());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteRole(UUID id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException("Role não encontrada com ID: " + id));

        if (role.isSystem()) {
            throw new IllegalStateException("Não é possível excluir uma role do sistema");
        }

        List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleId(id);
        for (RolePermission rp : rolePermissions) {
            rolePermissionRepository.deleteByRoleIdAndPermissionId(id, rp.getPermissionId());
        }

        roleRepository.deleteById(id);
        log.info("Role excluída: {}", role.getName());
    }

    @Override
    @Transactional
    public void assignPermissionToRole(UUID roleId, String permissionId) {
        roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException("Role não encontrada"));
        UUID permId = UUID.fromString(permissionId);
        permissionRepository.findById(permId)
                .orElseThrow(() -> new PermissionNotFoundException("Permissão não encontrada"));

        if (rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permId)) {
            return;
        }

        RolePermission rp = RolePermission.create(roleId, permId);
        rolePermissionRepository.save(rp);
        log.info("Permissão {} atribuída a role {}", permissionId, roleId);
    }

    @Override
    @Transactional
    public void removePermissionFromRole(UUID roleId, String permissionId) {
        UUID permId = UUID.fromString(permissionId);
        rolePermissionRepository.deleteByRoleIdAndPermissionId(roleId, permId);
        log.info("Permissão {} removida da role {}", permissionId, roleId);
    }

    @Override
    @Transactional
    public void assignRoleToUser(UUID userId, UUID roleId, UUID companyId) {
        if (userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            return;
        }
        UserRole userRole = UserRole.assign(userId, roleId, companyId);
        userRoleRepository.save(userRole);
        log.info("Role {} atribuída ao usuário {}", roleId, userId);
    }

    @Override
    @Transactional
    public void removeRoleFromUser(UUID userId, UUID roleId) {
        userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);
        log.info("Role {} removida do usuário {}", roleId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getUserRoles(UUID userId, UUID companyId) {
        return userRoleRepository.findByUserIdAndCompanyId(userId, companyId).stream()
                .map(ur -> {
                    Role role = roleRepository.findById(ur.getRoleId()).orElse(null);
                    return role != null ? mapToResponse(role) : null;
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private RoleResponse mapToResponse(Role role) {
        List<PermissionResponse> permissions = rolePermissionRepository.findByRoleId(role.getId()).stream()
                .map(rp -> permissionRepository.findById(rp.getPermissionId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(this::mapPermissionToResponse)
                .toList();

        return new RoleResponse(
                role.getId().toString(),
                role.getName().name(),
                role.getDescription(),
                role.getCompanyId() != null ? role.getCompanyId().toString() : null,
                role.isSystem(),
                role.isActive(),
                permissions,
                role.getCreatedAt(),
                role.getUpdatedAt()
        );
    }

    private PermissionResponse mapPermissionToResponse(Permission permission) {
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
