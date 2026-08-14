package com.becommerce.crm.infrastructure.identity.persistence;

import com.becommerce.crm.application.identity.port.output.PermissionRepository;
import com.becommerce.crm.application.identity.port.output.RolePermissionRepository;
import com.becommerce.crm.application.identity.port.output.RoleRepository;
import com.becommerce.crm.domain.identity.Permission;
import com.becommerce.crm.domain.identity.Role;
import com.becommerce.crm.domain.identity.RolePermission;
import com.becommerce.crm.domain.identity.valueobject.RoleName;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Seed de papéis (RBAC) de um tenant, reutilizado em tempo de startup
 * (RoleDataSeeder) e no onboarding (8.3, criação de empresa self-service).
 *
 * <p>Must run com {@code TenantContext} já definido para a empresa-alvo: as
 * policies RLS (V019) exigem {@code company_id = app.current_tenant_id()}
 * para INSERT em {@code roles} e {@code role_permissions}.
 */
@Service
public class RoleSeedService {

    private static final Logger log = LoggerFactory.getLogger(RoleSeedService.class);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public RoleSeedService(RoleRepository roleRepository,
                           PermissionRepository permissionRepository,
                           RolePermissionRepository rolePermissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    public void seedRoles(UUID companyId) {
        Map<RoleName, List<String>> rolePermissions = new EnumMap<>(RoleName.class);
        rolePermissions.put(RoleName.SUPER_ADMIN, List.of("*"));
        rolePermissions.put(RoleName.ADMIN, List.of(
            "user:create", "user:read", "user:update", "user:delete", "user:invite",
            "role:create", "role:read", "role:update", "role:delete", "role:assign",
            "permission:assign",
            "company:view", "company:create", "company:update",
            "dashboard:view",
            "lead:create", "lead:read", "lead:update", "lead:delete",
            "contact:create", "contact:read", "contact:update", "contact:delete",
            "pipeline:view", "pipeline:update",
            "opportunity:create", "opportunity:read", "opportunity:update", "opportunity:delete",
            "opportunity:move", "opportunity:win", "opportunity:lose",
            "chat:view", "chat:send",
            "campaign:create", "campaign:read", "campaign:update", "campaign:delete",
            "report:view", "report:export",
            "settings:view", "settings:update",
            "audit:read", "audit:export",
            "membership:view", "membership:manage"
        ));
        rolePermissions.put(RoleName.MANAGER, List.of(
            "user:read", "user:update",
            "role:read",
            "dashboard:view",
            "lead:create", "lead:read", "lead:update", "lead:delete",
            "contact:create", "contact:read", "contact:update", "contact:delete",
            "pipeline:view", "pipeline:update",
            "opportunity:create", "opportunity:read", "opportunity:update", "opportunity:delete",
            "opportunity:move", "opportunity:win", "opportunity:lose",
            "chat:view", "chat:send",
            "campaign:create", "campaign:read", "campaign:update", "campaign:delete",
            "report:view", "report:export",
            "settings:view",
            "audit:read"
        ));
        rolePermissions.put(RoleName.AGENT, List.of(
            "dashboard:view",
            "lead:create", "lead:read", "lead:update",
            "contact:create", "contact:read", "contact:update",
            "pipeline:view",
            "opportunity:create", "opportunity:read", "opportunity:update", "opportunity:move",
            "chat:view", "chat:send",
            "campaign:read",
            "report:view"
        ));
        rolePermissions.put(RoleName.VIEWER, List.of(
            "dashboard:view",
            "lead:read",
            "contact:read",
            "pipeline:view",
            "opportunity:read",
            "chat:view",
            "campaign:read",
            "report:view"
        ));

        for (Map.Entry<RoleName, List<String>> entry : rolePermissions.entrySet()) {
            RoleName roleName = entry.getKey();
            List<String> permNames = entry.getValue();

            Role role = roleRepository.findByNameAndCompanyId(roleName.name(), companyId).orElseGet(() -> {
                Role newRole = Role.createSystem(roleName.name(), companyId);
                newRole.setDescription(roleName.getDisplayName());
                newRole.setSystem(true);
                return roleRepository.save(newRole);
            });

            if (permNames.contains("*")) {
                continue;
            }

            for (String permName : permNames) {
                permissionRepository.findByName(permName).ifPresent(permission -> {
                    if (!rolePermissionRepository.existsByRoleIdAndPermissionId(role.getId(), permission.getId())) {
                        rolePermissionRepository.save(RolePermission.create(role.getId(), permission.getId()));
                    }
                });
            }
        }

        log.info("Role seeding completed (company={})", companyId);
    }
}