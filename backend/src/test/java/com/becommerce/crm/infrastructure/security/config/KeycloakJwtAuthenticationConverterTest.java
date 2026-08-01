package com.becommerce.crm.infrastructure.security.config;

import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakJwtAuthenticationConverterTest {

    private static final String SUB = "78490eac-150e-44db-b2c4-d7999c1c3801";
    private static final String EMAIL = "ghilherme007@gmail.com";

    @Mock private CurrentUserResolver currentUserResolver;

    private KeycloakJwtAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        converter = new KeycloakJwtAuthenticationConverter(currentUserResolver);
    }

    @Test
    void shouldBuildPrincipalWithCurrentUserAfterResolution() {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        CurrentUser currentUser = CurrentUser.fromKeycloak(
                userId, EMAIL, companyId, List.of("AGENT"),
                List.of("dashboard:view"), SUB, "Ghilherme Santos");

        when(currentUserResolver.resolve(any(Jwt.class))).thenReturn(currentUser);

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuer("http://keycloak")
                .subject(SUB)
                .claim("email", EMAIL)
                .claim("realm_access", java.util.Map.of("roles", List.of("user")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        UsernamePasswordAuthenticationToken authentication = converter.convert(jwt);

        assertNotNull(authentication);
        CurrentUser principal = (CurrentUser) authentication.getPrincipal();
        assertEquals(userId, principal.userId());
        assertEquals(companyId, principal.companyId());
        assertEquals(SUB, principal.keycloakSub());
        assertEquals(EMAIL, principal.email());
        assertTrue(principal.roles().contains("AGENT"));
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_AGENT")));
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("dashboard:view")));
    }

    @Test
    void shouldPropagateAuthenticationServiceExceptionFromResolver() {
        when(currentUserResolver.resolve(any(Jwt.class)))
                .thenThrow(new AuthenticationServiceException("Auto-provisioning indisponível"));

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuer("http://keycloak")
                .subject(SUB)
                .claim("email", EMAIL)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        assertThrows(AuthenticationServiceException.class, () -> converter.convert(jwt));
    }
}
