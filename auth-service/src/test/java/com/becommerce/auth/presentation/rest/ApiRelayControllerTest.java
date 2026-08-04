package com.becommerce.auth.presentation.rest;

import com.becommerce.auth.domain.gateway.OidcGatewayException;
import com.becommerce.auth.infrastructure.config.SecurityConfig;
import com.becommerce.auth.infrastructure.gateway.GatewayApiRelay;
import com.becommerce.auth.infrastructure.gateway.GatewayCookieFactory;
import com.becommerce.auth.infrastructure.observability.CorrelationIdContext;
import com.becommerce.auth.infrastructure.security.JwtAuthenticationEntryPoint;
import com.becommerce.auth.infrastructure.security.KeycloakIdentityConverter;
import com.becommerce.auth.presentation.rest.handler.GlobalExceptionHandler;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApiRelayController.class)
@Import({SecurityConfig.class, KeycloakIdentityConverter.class, JwtAuthenticationEntryPoint.class,
        GlobalExceptionHandler.class})
class ApiRelayControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private JwtDecoder jwtDecoder;
    @MockBean private GatewayApiRelay relay;
    @MockBean private GatewayCookieFactory cookieFactory;

    private Cookie sessionCookie() {
        return new Cookie("crm_session", "opaque-session-token");
    }

    @Test
    void shouldForwardWithSessionTokenFromCookie() throws Exception {
        when(cookieFactory.readSessionToken(any())).thenReturn(Optional.of("opaque-session-token"));
        when(relay.forward(eq(HttpMethod.GET), eq("/api/v1/users"), eq("page=1"),
                eq("opaque-session-token"), any(), any()))
                .thenReturn(ResponseEntity.ok("{\"data\":1}".getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(get("/api/v1/users").queryParam("page", "1").cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"data\":1}"));

        verify(relay).forward(eq(HttpMethod.GET), eq("/api/v1/users"), eq("page=1"),
                eq("opaque-session-token"), any(), any());
    }

    @Test
    void shouldReturn401JsonWhenSessionMissing() throws Exception {
        when(cookieFactory.readSessionToken(any())).thenReturn(Optional.empty());
        when(relay.forward(any(), any(), any(), any(), any(), any()))
                .thenThrow(new OidcGatewayException("SESSION_NOT_FOUND", 401,
                        "Sessão de gateway não encontrada."));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void shouldNotForwardSensitiveRequestHeaders() throws Exception {
        when(jwtDecoder.decode(any())).thenReturn(Jwt.withTokenValue("t")
                .header("alg", "none").subject("sub").claim("email", "a@b.com").build());
        when(cookieFactory.readSessionToken(any())).thenReturn(Optional.of("opaque-session-token"));
        when(relay.forward(any(), any(), any(), any(), any(), any()))
                .thenReturn(ResponseEntity.ok(new byte[0]));

        mockMvc.perform(get("/api/v1/users").cookie(sessionCookie())
                        .header("Authorization", "Bearer leaked")
                        .header("Cookie", "should-not-forward")
                        .header("Accept", "application/json"))
                .andExpect(status().isOk());

        verify(relay).forward(any(), any(), any(), any(),
                org.mockito.ArgumentMatchers.<Map<String, String>>argThat(headers ->
                        headers != null
                                && !headers.containsKey("Authorization")
                                && !headers.containsKey("Cookie")
                                && "application/json".equals(headers.get("Accept"))),
                any());
    }

    @Test
    void shouldPropagateCorrelationIdToBackend() throws Exception {
        CorrelationIdContext.set("corr-12345678");
        try {
            when(cookieFactory.readSessionToken(any())).thenReturn(Optional.of("opaque-session-token"));
            when(relay.forward(any(), any(), any(), any(), any(), any()))
                    .thenReturn(ResponseEntity.ok(new byte[0]));

            mockMvc.perform(get("/api/v1/users").cookie(sessionCookie()))
                    .andExpect(status().isOk());

            verify(relay).forward(any(), any(), any(), any(),
                    org.mockito.ArgumentMatchers.<Map<String, String>>argThat(headers ->
                            headers != null && "corr-12345678".equals(headers.get("X-Correlation-Id"))),
                    any());
        } finally {
            CorrelationIdContext.clear();
        }
    }
}
