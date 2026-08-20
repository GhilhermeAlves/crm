package com.becommerce.crm.infrastructure.identity.persistence;

import com.becommerce.crm.application.identity.port.output.PermissionRepository;
import com.becommerce.crm.application.identity.port.output.RolePermissionRepository;
import com.becommerce.crm.application.identity.port.output.RoleRepository;
import com.becommerce.crm.domain.identity.Permission;
import com.becommerce.crm.domain.identity.Role;
import com.becommerce.crm.domain.identity.RolePermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifica o comportamento do wildcard "*" no {@link RoleSeedService}: SUPER_ADMIN
 * deve receber TODAS as permissões existentes (não apenas um subconjunto fixo).
 * Regressão do bug em que "*" era um no-op e o SUPER_ADMIN ficava sem ai:chat /
 * ai:suggest (403 em /api/v1/ai/*).
 */
@ExtendWith(MockitoExtension.class)
class RoleSeedServiceTest {

    private static final UUID COMPANY = UUID.fromString("db8115d1-40d0-4868-8d70-d95fa16a934f");

    @Mock RoleRepository roleRepository;
    @Mock PermissionRepository permissionRepository;
    @Mock RolePermissionRepository rolePermissionRepository;
    @Mock Permission contactRead;
    @Mock Permission aiChat;
    @Mock Permission aiSuggest;

    @InjectMocks RoleSeedService roleSeedService;

    private Role superAdminRole;

    @BeforeEach
    void setUp() {
        superAdminRole = Role.createSystem("SUPER_ADMIN", COMPANY);
    }

    @Test
    void wildcardSuperAdminGrantsEveryExistingPermission() {
        when(roleRepository.findByNameAndCompanyId("SUPER_ADMIN", COMPANY))
                .thenReturn(Optional.of(superAdminRole));
        when(permissionRepository.findAll())
                .thenReturn(List.of(contactRead, aiChat, aiSuggest));
        when(contactRead.getId()).thenReturn(UUID.randomUUID());
        when(aiChat.getId()).thenReturn(UUID.randomUUID());
        when(aiSuggest.getId()).thenReturn(UUID.randomUUID());
        when(rolePermissionRepository.existsByRoleIdAndPermissionId(any(), any()))
                .thenReturn(false);

        roleSeedService.seedRoles(COMPANY);

        verify(rolePermissionRepository, org.mockito.Mockito.times(3)).save(any(RolePermission.class));
    }

    @Test
    void wildcardSuperAdminSkipsAlreadyGrantedPermissions() {
        when(roleRepository.findByNameAndCompanyId("SUPER_ADMIN", COMPANY))
                .thenReturn(Optional.of(superAdminRole));
        when(permissionRepository.findAll()).thenReturn(List.of(aiChat));
        when(aiChat.getId()).thenReturn(UUID.randomUUID());
        // ai:chat já concedido; os demais (inexistentes na lista) nada a fazer.
        when(rolePermissionRepository.existsByRoleIdAndPermissionId(superAdminRole.getId(), aiChat.getId()))
                .thenReturn(true);

        roleSeedService.seedRoles(COMPANY);

        verify(rolePermissionRepository, never()).save(any(RolePermission.class));
    }
}