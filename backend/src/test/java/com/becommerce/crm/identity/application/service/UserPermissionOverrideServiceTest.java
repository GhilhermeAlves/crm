package com.becommerce.crm.identity.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.identity.application.dto.UserPermissionsResponse;
import com.becommerce.crm.identity.application.port.output.PermissionRepository;
import com.becommerce.crm.identity.application.port.output.RoleRepository;
import com.becommerce.crm.identity.application.port.output.UserPermissionOverrideRepository;
import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.identity.application.port.output.UserRoleRepository;
import com.becommerce.crm.identity.domain.Permission;
import com.becommerce.crm.identity.domain.Role;
import com.becommerce.crm.identity.domain.User;
import com.becommerce.crm.identity.domain.UserRole;
import com.becommerce.crm.identity.domain.valueobject.Email;
import com.becommerce.crm.identity.domain.valueobject.Password;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPermissionOverrideServiceTest {

    private static final UUID COMPANY_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID USER_ID = UUID.fromString("22222222-3333-4444-5555-666666666666");
    private static final UUID ROLE_ID = UUID.fromString("33333333-4444-5555-6666-777777777777");

    @Mock private UserRepository userRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private UserPermissionOverrideRepository overrideRepository;
    @Mock private TenantAuditRecorder auditor;

    @Test
    void deveIncluirPermissoesDosPerfisMesmoQuandoNegadasPorOverride() {
        User user = User.create(new Email("ana@example.com"), new Password("Kc!Valid1Aa1"),
                "Ana", "Silva", COMPANY_ID);
        Role role = Role.create("Administradora", COMPANY_ID);
        role.setId(ROLE_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRoleRepository.findByUserIdAndCompanyId(USER_ID, COMPANY_ID))
                .thenReturn(List.of(UserRole.assign(USER_ID, ROLE_ID, COMPANY_ID)));
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role));
        when(permissionRepository.findByRoleId(ROLE_ID)).thenReturn(List.of(
                Permission.create("finance:read", "Ler finanças", "finance", "finance", "read"),
                Permission.create("finance:manage", "Gerenciar finanças", "finance", "finance", "manage")));
        when(permissionRepository.findEffectivePermissionNamesByUserIdAndCompanyId(USER_ID, COMPANY_ID))
                .thenReturn(List.of("finance:manage", "billing:read"));
        when(overrideRepository.findByUser(USER_ID, COMPANY_ID)).thenReturn(List.of(
                new UserPermissionOverrideRepository.Entry(UUID.randomUUID(), "finance:read", "DENY"),
                new UserPermissionOverrideRepository.Entry(UUID.randomUUID(), "billing:read", "ALLOW")));

        UserPermissionsResponse response = new UserPermissionOverrideService(
                userRepository, userRoleRepository, roleRepository, permissionRepository,
                overrideRepository, auditor).listPermissions(COMPANY_ID, USER_ID);

        assertEquals(List.of("finance:manage", "finance:read"), response.rolePermissionNames());
        assertFalse(response.rolePermissionNames().contains("billing:read"));
    }
}
