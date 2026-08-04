package com.becommerce.auth.presentation.rest;

import com.becommerce.auth.application.gateway.port.input.GatewayOidcUseCase;
import com.becommerce.auth.domain.gateway.GatewaySession;
import com.becommerce.auth.domain.gateway.OidcGatewayException;
import com.becommerce.auth.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.auth.infrastructure.config.SecurityConfig;
import com.becommerce.auth.infrastructure.gateway.GatewayCookieFactory;
import com.becommerce.auth.infrastructure.security.JwtAuthenticationEntryPoint;
import com.becommerce.auth.infrastructure.security.KeycloakIdentityConverter;
import com.becommerce.auth.presentation.rest.handler.GlobalExceptionHandler;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OidcGatewayController.class)
@Import({SecurityConfig.class, KeycloakIdentityConverter.class, JwtAuthenticationEntryPoint.class,
        GlobalExceptionHandler.class})
class OidcGatewayControllerTest {

    private static final String AUTH_URI = "https://srv1348261.hstgr.cloud/realms/CRM/protocol/openid-connect/auth?response_type=code";
    private static final String END_SESSION_URI = "https://srv1348261.hstgr.cloud/realms/CRM/protocol/openid-connect/logout";
    private static final UUID USER_ID = UUID.fromString("974bbedb-298d-4ec6-a037-514b24c248e4");
    private static final UUID COMPANY_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Autowired private MockMvc mockMvc;

    @MockBean private JwtDecoder jwtDecoder;
    @MockBean private GatewayOidcUseCase gatewayOidcUseCase;
    @MockBean private GatewayCookieFactory cookieFactory;

    private GatewaySession session() {
        Instant now = Instant.now();
        return new GatewaySession("opaque-session-token", USER_ID, "a@b.com", COMPANY_ID, COMPANY_ID,
                List.of("AGENT"), List.of(), "sub", "sid", "keycloak", "Ghilherme",
                now, now.plusSeconds(3600), now, "id-token-hint", "access", "refresh",
                now.plusSeconds(300), "csrf-token", null);
    }

    private Cookie sessionCookie() {
        return new Cookie("crm_session", "opaque-session-token");
    }

    private void stubCookies() {
        when(cookieFactory.createSessionCookie(anyString()))
                .thenReturn(ResponseCookie.from("crm_session", "opaque").build());
        when(cookieFactory.createCsrfCookie(anyString()))
                .thenReturn(ResponseCookie.from("XSRF-TOKEN", "csrf").build());
    }

    @Test
    void shouldRedirectToKeycloakAuthorizationEndpoint() throws Exception {
        when(gatewayOidcUseCase.beginAuthorization("/dashboard"))
                .thenReturn(new GatewayOidcUseCase.BeginAuthorization(AUTH_URI, "/dashboard"));

        mockMvc.perform(get("/auth/authorize").param("redirect", "/dashboard"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(AUTH_URI));
    }

    @Test
    void shouldRejectOpenRedirectWith400() throws Exception {
        when(gatewayOidcUseCase.beginAuthorization("//evil.example"))
                .thenThrow(new OidcGatewayException("OPEN_REDIRECT", 400, "Redirect não permitido pela allowlist."));

        mockMvc.perform(get("/auth/authorize").param("redirect", "//evil.example"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OPEN_REDIRECT"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldSetSessionAndCsrfCookieAndRedirectAfterSuccessfulLogin() throws Exception {
        when(gatewayOidcUseCase.completeAuthorization("code-1", "state-1"))
                .thenReturn(new GatewayOidcUseCase.AuthenticationResult(session(), "/dashboard"));
        when(cookieFactory.createSessionCookie("opaque-session-token"))
                .thenReturn(ResponseCookie.from("crm_session", "opaque-session-token")
                        .httpOnly(true).path("/").maxAge(java.time.Duration.ofHours(8)).build());
        when(cookieFactory.createCsrfCookie("csrf-token"))
                .thenReturn(ResponseCookie.from("XSRF-TOKEN", "csrf-token")
                        .path("/").maxAge(java.time.Duration.ofHours(8)).build());

        mockMvc.perform(get("/auth/callback").param("code", "code-1").param("state", "state-1"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("crm_session=")));
    }

    @Test
    void shouldRejectCallbackWithOidcError() throws Exception {
        mockMvc.perform(get("/auth/callback").param("error", "access_denied"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OIDC_ERROR"))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void shouldRejectProvisioningRequiredWithoutCookie() throws Exception {
        when(gatewayOidcUseCase.completeAuthorization(anyString(), anyString()))
                .thenThrow(new OidcGatewayException("PROVISIONING_REQUIRED", 403, "sem usuário CRM"));

        mockMvc.perform(get("/auth/callback").param("code", "c").param("state", "s"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROVISIONING_REQUIRED"))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void shouldRejectCrmAccessDeniedWithoutCookie() throws Exception {
        when(gatewayOidcUseCase.completeAuthorization(anyString(), anyString()))
                .thenThrow(new CrmAccessDeniedException("Usuário sem acesso ao CRM (crm_enabled=false): conceda acesso explicitamente."));

        mockMvc.perform(get("/auth/callback").param("code", "c").param("state", "s"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CRM_ACCESS_DENIED"))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void shouldKeepAuthorizeAndCallbackPublic() throws Exception {
        stubCookies();
        when(gatewayOidcUseCase.beginAuthorization(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new GatewayOidcUseCase.BeginAuthorization(AUTH_URI, "/"));
        when(gatewayOidcUseCase.completeAuthorization("c", "s"))
                .thenReturn(new GatewayOidcUseCase.AuthenticationResult(session(), "/"));

        mockMvc.perform(get("/auth/authorize"))
                .andExpect(status().isFound());
        mockMvc.perform(get("/auth/callback").param("code", "c").param("state", "s"))
                .andExpect(status().isFound());
    }

    @Test
    void shouldRedirectToEndSessionAndClearCookieOnLogout() throws Exception {
        when(cookieFactory.readSessionToken(any())).thenReturn(java.util.Optional.of("opaque-session-token"));
        when(gatewayOidcUseCase.logout("opaque-session-token", null))
                .thenReturn(new GatewayOidcUseCase.LogoutResult(END_SESSION_URI + "?id_token_hint=hint"));
        when(cookieFactory.createExpiredSessionCookie())
                .thenReturn(ResponseCookie.from("crm_session", "").maxAge(java.time.Duration.ZERO).build());

        mockMvc.perform(get("/auth/logout").cookie(sessionCookie()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(END_SESSION_URI + "?id_token_hint=hint"))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("crm_session=")));
    }

    @Test
    void shouldClearCookieAndRedirectLocallyWhenNoSessionOnLogout() throws Exception {
        when(cookieFactory.readSessionToken(any())).thenReturn(java.util.Optional.empty());
        when(gatewayOidcUseCase.logout(null, "/dashboard"))
                .thenReturn(new GatewayOidcUseCase.LogoutResult("/dashboard"));
        when(cookieFactory.createExpiredSessionCookie())
                .thenReturn(ResponseCookie.from("crm_session", "").maxAge(java.time.Duration.ZERO).build());

        mockMvc.perform(get("/auth/logout").param("post_logout_redirect_uri", "/dashboard"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void shouldRefreshSessionAndReturnNoContent() throws Exception {
        when(cookieFactory.readSessionToken(any())).thenReturn(java.util.Optional.of("opaque-session-token"));
        when(gatewayOidcUseCase.refresh("opaque-session-token"))
                .thenReturn(new GatewayOidcUseCase.RefreshResult(session()));

        mockMvc.perform(post("/auth/refresh").cookie(sessionCookie()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldRejectRefreshWhenSessionMissing() throws Exception {
        when(cookieFactory.readSessionToken(any())).thenReturn(java.util.Optional.empty());
        when(gatewayOidcUseCase.refresh(null))
                .thenThrow(new OidcGatewayException("SESSION_NOT_FOUND", 401, "Sessão de gateway não encontrada."));

        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
    }

    @Test
    void shouldRejectGetOnRefreshEndpoint() throws Exception {
        mockMvc.perform(get("/auth/refresh").cookie(sessionCookie()))
                .andExpect(status().isMethodNotAllowed());

        verify(gatewayOidcUseCase, never()).refresh(anyString());
    }

    @Test
    void shouldStillRequireAuthenticationForInternalCurrentUser() throws Exception {
        mockMvc.perform(get("/internal/auth/current-user"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }
}
