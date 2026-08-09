package com.becommerce.crm.infrastructure.security.config;

import com.becommerce.crm.infrastructure.identity.client.AuthServiceClient;
import com.becommerce.crm.infrastructure.identity.client.dto.CurrentUserDto;
import com.becommerce.crm.infrastructure.identity.client.dto.ResolutionResponse;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceCurrentUserResolverTest {

    private static final String SUB = "78490eac-150e-44db-b2c4-d7999c1c3801";
    private static final String EMAIL = "ghilherme007@gmail.com";

    @Mock private AuthServiceClient authServiceClient;
    @Mock private LocalCurrentUserResolver localCurrentUserResolver;

    @Test
    void shouldReturnResolvedCurrentUserFromAuthService() {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        CurrentUserDto dto = new CurrentUserDto(
                userId, EMAIL, companyId, companyId, List.of("AGENT"),
                List.of("dashboard:view"), SUB, null, "keycloak", "Ghilherme", null);
        when(authServiceClient.currentUser("jwt-token"))
                .thenReturn(new ResolutionResponse("RESOLVED", dto, null));

        AuthServiceCurrentUserResolver resolver =
                new AuthServiceCurrentUserResolver(authServiceClient, localCurrentUserResolver);

        CurrentUser currentUser = resolver.resolve(jwt("jwt-token"));

        assertNotNull(currentUser);
        assertEquals(userId, currentUser.userId());
        assertEquals(companyId, currentUser.companyId());
        assertEquals(EMAIL, currentUser.email());
        assertTrue(currentUser.roles().contains("AGENT"));
    }

    @Test
    void shouldFallbackToLocalWhenProvisioningRequired() {
        when(authServiceClient.currentUser("jwt-token"))
                .thenReturn(new ResolutionResponse("PROVISIONING_REQUIRED", null, null));

        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        CurrentUser local = CurrentUser.fromKeycloak(
                userId, EMAIL, companyId, List.of("AGENT"), List.of(), SUB, "Ghilherme");
        when(localCurrentUserResolver.resolve(any(Jwt.class))).thenReturn(local);

        AuthServiceCurrentUserResolver resolver =
                new AuthServiceCurrentUserResolver(authServiceClient, localCurrentUserResolver);

        CurrentUser currentUser = resolver.resolve(jwt("jwt-token"));

        assertEquals(userId, currentUser.userId());
        verify(localCurrentUserResolver).resolve(any(Jwt.class));
    }

    @Test
    void shouldFallbackToLocalWhenAuthServiceFails() {
        when(authServiceClient.currentUser(eq("jwt-token")))
                .thenThrow(new RuntimeException("connection refused"));

        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        CurrentUser local = CurrentUser.fromKeycloak(
                userId, EMAIL, companyId, List.of(), List.of(), SUB, null);
        when(localCurrentUserResolver.resolve(any(Jwt.class))).thenReturn(local);

        AuthServiceCurrentUserResolver resolver =
                new AuthServiceCurrentUserResolver(authServiceClient, localCurrentUserResolver);

        CurrentUser currentUser = resolver.resolve(jwt("jwt-token"));

        assertEquals(userId, currentUser.userId());
        verify(localCurrentUserResolver).resolve(any(Jwt.class));
    }

    private static Jwt jwt(String tokenValue) {
        return Jwt.withTokenValue(tokenValue)
                .header("alg", "none")
                .issuer("http://keycloak")
                .subject(SUB)
                .claim("email", EMAIL)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
