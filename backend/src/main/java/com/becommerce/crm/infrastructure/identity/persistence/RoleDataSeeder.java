package com.becommerce.crm.infrastructure.identity.persistence;

import com.becommerce.crm.domain.identity.Permission;
import com.becommerce.crm.domain.identity.Role;
import com.becommerce.crm.domain.identity.RolePermission;
import com.becommerce.crm.application.identity.port.output.PermissionRepository;
import com.becommerce.crm.application.identity.port.output.RolePermissionRepository;
import com.becommerce.crm.application.identity.port.output.RoleRepository;
import com.becommerce.crm.domain.identity.valueobject.RoleName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Order(1)
public class RoleDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RoleDataSeeder.class);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public RoleDataSeeder(RoleRepository roleRepository,
                          PermissionRepository permissionRepository,
                          RolePermissionRepository rolePermissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Override
    public void run(String... args) {
        seedRoles();
    }

    private void seedRoles() {
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
            "chat:view", "chat:send",
            "campaign:create", "campaign:read", "campaign:update", "campaign:delete",
            "report:view", "report:export",
            "settings:view", "settings:update",
            "audit:read", "audit:export"
        ));
        rolePermissions.put(RoleName.MANAGER, List.of(
            "user:read", "user:update",
            "role:read",
            "dashboard:view",
            "lead:create", "lead:read", "lead:update", "lead:delete",
            "contact:create", "contact:read", "contact:update", "contact:delete",
            "pipeline:view", "pipeline:update",
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
            "chat:view", "chat:send",
            "campaign:read",
            "report:view"
        ));
        rolePermissions.put(RoleName.VIEWER, List.of(
            "dashboard:view",
            "lead:read",
            "contact:read",
            "pipeline:view",
            "chat:view",
            "campaign:read",
            "report:view"
        ));

        for (Map.Entry<RoleName, List<String>> entry : rolePermissions.entrySet()) {
            RoleName roleName = entry.getKey();
            List<String> permNames = entry.getValue();

            Role role = roleRepository.findByNameAndCompanyId(roleName, null).orElseGet(() -> {
                Role newRole = Role.createSystem(roleName);
                newRole.setDescription(roleName.getDisplayName());
                newRole.setSystem(true);
                Role saved = roleRepository.save(newRole);
                log.info("Seeded system role: {}", roleName);
                return saved;
            });

            if (permNames.contains("*")) {
                continue;
            }

            for (String permName : permNames) {
                permissionRepository.findByName(permName).ifPresent(permission -> {
                    if (!rolePermissionRepository.existsByRoleIdAndPermissionId(role.getId(), permission.getId())) {
                        RolePermission rp = RolePermission.create(role.getId(), permission.getId());
                        rolePermissionRepository.save(rp);
                    }
                });
            }
        }

        log.info("Role seeding completed");
    }
}
