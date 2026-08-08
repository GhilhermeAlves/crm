package com.becommerce.crm.presentation.rest.internal;

import com.becommerce.crm.application.identity.service.AuthService;
import com.becommerce.crm.domain.identity.exception.InvalidCredentialsException;
import com.becommerce.crm.domain.identity.exception.LinkingRequiredException;
import com.becommerce.crm.domain.identity.exception.UserProvisioningException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sprint 7.2 — endpoints internos de identidade consumidos pelo crm-auth-service.
 */
@ExtendWith(MockitoExtension.class)
class IdentityInternalControllerTest {

    private static final String SUB = "google-78490eac-150e-44db-b2c4-d7999c1c3801";
    private static final String EMAIL = "ghilherme007@gmail.com";

    @Mock private AuthService authService;

    @InjectMocks
    private IdentityInternalController controller;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Jwt externalVerifiedJwt() {
        return Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .subject(SUB)
                .claim("email", EMAIL)
                .claim("preferred_username", "ghilherme007")
                .claim("given_name", "Ghilherme")
                .claim("family_name", "Santos")
                .claim("identity_provider", "google")
                .claim("email_verified", true)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    void shouldProvisionExternalIdentity() {
        ResponseEntity<IdentityInternalController.InternalIdentityResponse> response =
                controller.provision(externalVerifiedJwt());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("PROVISIONED", response.getBody().status());
        assertEquals(EMAIL, response.getBody().email());
        verify(authService).provisionKeycloakUser(eq(SUB), eq(EMAIL), eq("ghilherme007"),
                eq("Ghilherme"), eq("Santos"), eq("google"));
        assertNull(TenantContext.getKeycloakSub());
        assertNull(TenantContext.getIdentityEmail());
    }

    @Test
    void shouldSignalLinkingRequiredWhenEmailMatchesLocalAccount() {
        when(authService.provisionKeycloakUser(any(), any(), any(), any(), any(), any()))
                .thenThrow(new LinkingRequiredException("vinculação exige verificação explícita."));

        ResponseEntity<IdentityInternalController.InternalIdentityResponse> response =
                controller.provision(externalVerifiedJwt());

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("LINKING_REQUIRED", response.getBody().status());
    }

    @Test
    void shouldLinkAfterPasswordVerification() {
        ResponseEntity<IdentityInternalController.InternalIdentityResponse> response =
                controller.link(externalVerifiedJwt(), new IdentityInternalController.LinkRequest("segredo"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("LINKED", response.getBody().status());
        verify(authService).linkKeycloakIdentity(eq(SUB), eq(EMAIL), eq("Ghilherme"), eq("Santos"), eq("segredo"));
    }

    @Test
    void shouldRejectWrongPasswordOnLink() {
        when(authService.linkKeycloakIdentity(any(), any(), any(), any(), any()))
                .thenThrow(new InvalidCredentialsException());

        ResponseEntity<IdentityInternalController.InternalIdentityResponse> response =
                controller.link(externalVerifiedJwt(), new IdentityInternalController.LinkRequest("errada"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("INVALID_CREDENTIALS", response.getBody().status());
    }

    @Test
    void shouldReportMissingLocalAccountOnLink() {
        when(authService.linkKeycloakIdentity(any(), any(), any(), any(), any()))
                .thenThrow(new UserProvisioningException("Conta local não encontrada."));

        ResponseEntity<IdentityInternalController.InternalIdentityResponse> response =
                controller.link(externalVerifiedJwt(), new IdentityInternalController.LinkRequest("segredo"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("LINK_NOT_FOUND", response.getBody().status());
    }

    @Test
    void shouldRejectProvisionWhenExternalEmailNotVerified() {
        Jwt unverified = Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .subject(SUB)
                .claim("email", EMAIL)
                .claim("identity_provider", "google")
                .claim("email_verified", false)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        assertThrows(UserProvisioningException.class, () -> controller.provision(unverified));
        assertTrue(true);
    }
}
