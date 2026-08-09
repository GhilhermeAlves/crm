package com.becommerce.crm.infrastructure.security.config;

import com.becommerce.crm.application.identity.port.input.AuthUseCase;
import com.becommerce.crm.application.identity.port.output.PermissionRepository;
import com.becommerce.crm.application.identity.port.output.RolePermissionRepository;
import com.becommerce.crm.application.identity.port.output.RoleRepository;
import com.becommerce.crm.application.identity.port.output.UserRoleRepository;
import com.becommerce.crm.application.membership.port.output.MembershipRepository;
import com.becommerce.crm.domain.identity.Permission;
import com.becommerce.crm.domain.identity.Role;
import com.becommerce.crm.domain.identity.RolePermission;
import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.identity.UserRole;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.crm.domain.identity.exception.UserProvisioningException;
import com.becommerce.crm.domain.identity.valueobject.Email;
import com.becommerce.crm.domain.identity.valueobject.Password;
import com.becommerce.crm.domain.identity.valueobject.RoleName;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalCurrentUserResolverTest {

    private static final String SUB = "78490eac-150e-44db-b2c4-d7999c1c3801";
    private static final String EMAIL = "ghilherme007@gmail.com";

    @Mock private AuthUseCase authUseCase;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RolePermissionRepository rolePermissionRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private MembershipRepository membershipRepository;

    private LocalCurrentUserResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new LocalCurrentUserResolver(
                authUseCase, userRoleRepository, roleRepository, rolePermissionRepository, permissionRepository, membershipRepository);
    }

    @Test
    void shouldResolveCurrentUserAfterProvisioning() {
        UUID companyId = UUID.randomUUID();
        User user = User.create(new Email(EMAIL), new Password("Kc!Valid1Aa1"), "Ghilherme", "Santos", companyId);
        user.linkKeycloak(SUB);

        Role agentRole = Role.createSystem(RoleName.AGENT);
        Permission dashboardView = Permission.create("dashboard:view", "Visualizar dashboard", "dashboard", "dashboard", "view");

        when(authUseCase.provisionKeycloakUser(eq(SUB), eq(EMAIL), any(), any(), any(), any())).thenReturn(user);
        when(userRoleRepository.findByUserIdAndCompanyId(user.getId(), companyId))
                .thenReturn(List.of(UserRole.assign(user.getId(), agentRole.getId(), companyId)));
        when(roleRepository.findById(agentRole.getId())).thenReturn(Optional.of(agentRole));
        when(roleRepository.findByNameAndCompanyId(RoleName.AGENT, companyId)).thenReturn(Optional.of(agentRole));
        when(rolePermissionRepository.findByRoleId(agentRole.getId()))
                .thenReturn(List.of(RolePermission.create(agentRole.getId(), dashboardView.getId())));
        when(permissionRepository.findById(dashboardView.getId())).thenReturn(Optional.of(dashboardView));
        when(membershipRepository.existsActiveByUserIdAndCompanyId(user.getId(), companyId)).thenReturn(true);
        when(membershipRepository.findMembershipRoleByUserIdAndCompanyId(user.getId(), companyId))
                .thenReturn(Optional.of("AGENT"));

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuer("http://keycloak")
                .subject(SUB)
                .claim("email", EMAIL)
                .claim("preferred_username", EMAIL)
                .claim("given_name", "Ghilherme")
                .claim("family_name", "Santos")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        CurrentUser currentUser = resolver.resolve(jwt);

        assertNotNull(currentUser);
        assertEquals(user.getId(), currentUser.userId());
        assertEquals(companyId, currentUser.companyId());
        assertEquals(companyId, currentUser.tenantId());
        assertEquals(SUB, currentUser.keycloakSub());
        assertEquals(EMAIL, currentUser.email());
        assertTrue(currentUser.roles().contains("AGENT"));
        assertTrue(currentUser.permissions().contains("dashboard:view"));
        assertEquals("keycloak", currentUser.provider());
    }

    @Test
    void shouldBuildDisplayNameFromFullNameClaim() {
        UUID companyId = UUID.randomUUID();
        User user = User.create(new Email(EMAIL), new Password("Kc!Valid1Aa1"), "Ghilherme", "Santos", companyId);
        user.linkKeycloak(SUB);

        when(authUseCase.provisionKeycloakUser(eq(SUB), eq(EMAIL), any(), any(), any(), any())).thenReturn(user);
        when(userRoleRepository.findByUserIdAndCompanyId(user.getId(), companyId)).thenReturn(List.of());
        when(membershipRepository.existsActiveByUserIdAndCompanyId(user.getId(), companyId)).thenReturn(true);

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuer("http://keycloak")
                .subject(SUB)
                .claim("email", EMAIL)
                .claim("name", "Ghilherme Pereira Santos")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        CurrentUser currentUser = resolver.resolve(jwt);

        assertEquals("Ghilherme Pereira Santos", currentUser.displayName());
    }

    @Test
    void shouldTranslateProvisioningFailureToAuthenticationException() {
        when(authUseCase.provisionKeycloakUser(eq(SUB), eq(EMAIL), any(), any(), any(), any()))
                .thenThrow(new UserProvisioningException("Auto-provisioning indisponível"));

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuer("http://keycloak")
                .subject(SUB)
                .claim("email", EMAIL)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        assertThrows(AuthenticationServiceException.class, () -> resolver.resolve(jwt));
    }

    @Test
    void shouldTranslateCrmAccessDeniedToCrmAccessDeniedAuthenticationException() {
        when(authUseCase.provisionKeycloakUser(eq(SUB), eq(EMAIL), any(), any(), any(), any()))
                .thenThrow(new CrmAccessDeniedException("Usuário sem acesso ao CRM (crm_enabled=false)"));

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuer("http://keycloak")
                .subject(SUB)
                .claim("email", EMAIL)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        assertThrows(CrmAccessDeniedAuthenticationException.class, () -> resolver.resolve(jwt));
    }
}
