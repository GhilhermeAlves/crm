package com.becommerce.auth.presentation.rest;

import com.becommerce.auth.application.identity.port.input.CurrentUserResolutionUseCase;
import com.becommerce.auth.domain.identity.AuthenticatedIdentity;
import com.becommerce.auth.domain.identity.CurrentUser;
import com.becommerce.auth.domain.identity.CurrentUserResolution;
import com.becommerce.auth.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.auth.infrastructure.config.SecurityConfig;
import com.becommerce.auth.infrastructure.security.JwtAuthenticationEntryPoint;
import com.becommerce.auth.infrastructure.security.KeycloakIdentityConverter;
import com.becommerce.auth.presentation.rest.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({InternalAuthController.class, HealthController.class})
@Import({SecurityConfig.class, KeycloakIdentityConverter.class, JwtAuthenticationEntryPoint.class,
        GlobalExceptionHandler.class})
class InternalAuthControllerTest {

    private static final UUID USER_ID = UUID.fromString("974bbedb-298d-4ec6-a037-514b24c248e4");
    private static final UUID COMPANY_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final String SUB = "78490eac-150e-44db-b2c4-d7999c1c3801";
    private static final String EMAIL = "ghilherme007@gmail.com";

    @Autowired private MockMvc mockMvc;

    @MockBean private JwtDecoder jwtDecoder;
    @MockBean private CurrentUserResolutionUseCase currentUserResolutionUseCase;

    @BeforeEach
    void setUp() {
        when(jwtDecoder.decode(anyString())).thenReturn(authenticatedJwt());
    }

    private Jwt authenticatedJwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuer("https://srv1348261.hstgr.cloud/realms/CRM")
                .subject(SUB)
                .claim("email", EMAIL)
                .claim("preferred_username", EMAIL)
                .claim("given_name", "Ghilherme")
                .claim("family_name", "Santos")
                .claim("name", "Ghilherme Santos")
                .claim("sid", "session-9")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    private AuthenticatedIdentity identityFromJwt() {
        return new AuthenticatedIdentity(SUB, EMAIL, EMAIL, "Ghilherme", "Santos", "Ghilherme Santos", "session-9");
    }

    @Test
    void shouldResolveCurrentUserForAuthenticatedExistingUser() throws Exception {
        CurrentUser currentUser = new CurrentUser(USER_ID, EMAIL, COMPANY_ID, COMPANY_ID,
                List.of("AGENT"), List.of("contact:read", "dashboard:view"), SUB,
                "session-9", "keycloak", "Ghilherme Santos");
        when(currentUserResolutionUseCase.resolve(any(AuthenticatedIdentity.class)))
                .thenReturn(new CurrentUserResolution.Resolved(currentUser));

        mockMvc.perform(get("/internal/auth/current-user")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.currentUser.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.currentUser.email").value(EMAIL))
                .andExpect(jsonPath("$.currentUser.companyId").value(COMPANY_ID.toString()))
                .andExpect(jsonPath("$.currentUser.tenantId").value(COMPANY_ID.toString()))
                .andExpect(jsonPath("$.currentUser.roles[0]").value("AGENT"))
                .andExpect(jsonPath("$.currentUser.permissions[1]").value("dashboard:view"))
                .andExpect(jsonPath("$.currentUser.keycloakSub").value(SUB))
                .andExpect(jsonPath("$.currentUser.provider").value("keycloak"))
                .andExpect(jsonPath("$.currentUser.sessionId").value("session-9"));
    }

    @Test
    void shouldReturnProvisioningRequiredContractForUnknownUser() throws Exception {
        when(currentUserResolutionUseCase.resolve(any(AuthenticatedIdentity.class)))
                .thenReturn(new CurrentUserResolution.ProvisioningRequired(identityFromJwt()));

        mockMvc.perform(get("/internal/auth/current-user")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROVISIONING_REQUIRED"))
                .andExpect(jsonPath("$.identity.keycloakSub").value(SUB))
                .andExpect(jsonPath("$.identity.email").value(EMAIL));
    }

    @Test
    void shouldRejectUserWithoutCrmAccess() throws Exception {
        when(currentUserResolutionUseCase.resolve(any(AuthenticatedIdentity.class)))
                .thenThrow(new CrmAccessDeniedException("Usuário sem acesso ao CRM (crm_enabled=false): conceda acesso explicitamente."));

        mockMvc.perform(get("/internal/auth/current-user")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CRM_ACCESS_DENIED"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Usuário sem acesso ao CRM (crm_enabled=false): conceda acesso explicitamente."));
    }

    @Test
    void shouldRejectRequestWithoutAuthenticatedIdentity() throws Exception {
        mockMvc.perform(get("/internal/auth/current-user"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void shouldReportHealth() throws Exception {
        mockMvc.perform(get("/auth/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void shouldIgnoreArbitraryUserIdFromClient() throws Exception {
        UUID arbitraryUserId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(USER_ID, EMAIL, COMPANY_ID, COMPANY_ID,
                List.of("AGENT"), List.of(), SUB, null, "keycloak", "Ghilherme Santos");
        when(currentUserResolutionUseCase.resolve(any(AuthenticatedIdentity.class)))
                .thenReturn(new CurrentUserResolution.Resolved(currentUser));

        mockMvc.perform(get("/internal/auth/current-user")
                        .header("Authorization", "Bearer token")
                        .header("X-Current-User-Id", arbitraryUserId.toString())
                        .param("userId", arbitraryUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentUser.userId").value(USER_ID.toString()));

        ArgumentCaptor<AuthenticatedIdentity> captor = ArgumentCaptor.forClass(AuthenticatedIdentity.class);
        verify(currentUserResolutionUseCase).resolve(captor.capture());
        assertEquals(SUB, captor.getValue().keycloakSub());
        assertEquals(EMAIL, captor.getValue().email());
        assertNotEquals(arbitraryUserId.toString(), captor.getValue().keycloakSub());
    }
}
