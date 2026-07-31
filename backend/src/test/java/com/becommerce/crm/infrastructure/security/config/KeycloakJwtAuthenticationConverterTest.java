package com.becommerce.crm.infrastructure.security.config;

import com.becommerce.crm.application.identity.port.input.AuthUseCase;
import com.becommerce.crm.application.identity.port.output.PermissionRepository;
import com.becommerce.crm.application.identity.port.output.RolePermissionRepository;
import com.becommerce.crm.application.identity.port.output.RoleRepository;
import com.becommerce.crm.application.identity.port.output.UserRoleRepository;
import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.identity.exception.UserProvisioningException;
import com.becommerce.crm.domain.identity.valueobject.Email;
import com.becommerce.crm.domain.identity.valueobject.Password;
import com.becommerce.crm.infrastructure.security.filter.CrmPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakJwtAuthenticationConverterTest {

    private static final String SUB = "78490eac-150e-44db-b2c4-d7999c1c3801";
    private static final String EMAIL = "ghilherme007@gmail.com";

    @Mock private AuthUseCase authUseCase;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RolePermissionRepository rolePermissionRepository;
    @Mock private PermissionRepository permissionRepository;

    private KeycloakJwtAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        converter = new KeycloakJwtAuthenticationConverter(
                authUseCase, userRoleRepository, roleRepository, rolePermissionRepository, permissionRepository);
    }

    @Test
    void shouldBuildPrincipalWithNonNullUserIdAfterProvisioning() {
        UUID companyId = UUID.randomUUID();
        User user = User.create(new Email(EMAIL), new Password("Kc!Valid1Aa1"), "Ghilherme", "Santos", companyId);
        user.linkKeycloak(SUB);

        when(authUseCase.provisionKeycloakUser(eq(SUB), eq(EMAIL), any(), any(), any())).thenReturn(user);
        when(userRoleRepository.findByUserIdAndCompanyId(user.getId(), companyId)).thenReturn(List.of());

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuer("http://keycloak")
                .subject(SUB)
                .claim("email", EMAIL)
                .claim("preferred_username", EMAIL)
                .claim("given_name", "Ghilherme")
                .claim("family_name", "Santos")
                .claim("realm_access", java.util.Map.of("roles", List.of("user")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        UsernamePasswordAuthenticationToken authentication = converter.convert(jwt);

        assertNotNull(authentication);
        CrmPrincipal principal = (CrmPrincipal) authentication.getPrincipal();
        assertNotNull(principal.userId());
        assertEquals(user.getId(), principal.userId());
        assertEquals(companyId, principal.companyId());
        assertEquals(SUB, principal.keycloakSub());
        assertTrue(principal.roles().contains("USER"));
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void shouldTranslateProvisioningFailureToAuthenticationException() {
        when(authUseCase.provisionKeycloakUser(eq(SUB), eq(EMAIL), any(), any(), any()))
                .thenThrow(new UserProvisioningException("Auto-provisioning indisponível"));

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuer("http://keycloak")
                .subject(SUB)
                .claim("email", EMAIL)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        assertThrows(AuthenticationServiceException.class, () -> converter.convert(jwt));
        verify(authUseCase).provisionKeycloakUser(eq(SUB), eq(EMAIL), any(), any(), any());
    }
}
