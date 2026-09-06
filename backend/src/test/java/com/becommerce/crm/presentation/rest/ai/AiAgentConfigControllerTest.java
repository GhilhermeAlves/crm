package com.becommerce.crm.presentation.rest.ai;

import com.becommerce.crm.application.ai.dto.AgentConfigRequest;
import com.becommerce.crm.application.ai.dto.AgentConfigResponse;
import com.becommerce.crm.application.ai.port.input.AgentConfigUseCase;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import com.becommerce.crm.presentation.rest.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AiAgentConfigControllerTest {

    private static final UUID COMPANY_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID USER_ID = UUID.fromString("974bbedb-298d-4ec6-a037-514b24c248e4");

    @Mock private AgentConfigUseCase agentConfigUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AiAgentConfigController controller = new AiAgentConfigController(agentConfigUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void login(List<String> permissions) {
        CurrentUser principal = new CurrentUser(
                USER_ID, "admin@empresa.com", COMPANY_ID, COMPANY_ID,
                List.of("ADMIN"), permissions,
                "keycloak-sub", null, "keycloak", "Admin", "ADMIN");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal, null,
                        principal.permissions().stream().map(SimpleGrantedAuthority::new).toList()));
    }

    private void loginWithoutCompany(List<String> permissions) {
        CurrentUser noCompany = new CurrentUser(
                USER_ID, "admin@empresa.com", null, null,
                List.of(), permissions,
                "keycloak-sub", null, "keycloak", "Admin", null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        noCompany, null,
                        noCompany.permissions().stream().map(SimpleGrantedAuthority::new).toList()));
    }

    @Test
    void shouldReturnDefaultConfigWhenNotConfiguredYet() throws Exception {
        login(List.of("ai:agent-config"));
        when(agentConfigUseCase.get(COMPANY_ID)).thenReturn(new AgentConfigResponse(
                null, false, false, null, null, null, null, 60, 1000, null));

        mockMvc.perform(get("/api/v1/ai/agent-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.aiEnabled").value(false))
                .andExpect(jsonPath("$.allowAutoReply").value(false))
                .andExpect(jsonPath("$.cooldownMinutes").value(60))
                .andExpect(jsonPath("$.maxChars").value(1000));
    }

    @Test
    void shouldUpdateConfig() throws Exception {
        login(List.of("ai:agent-config"));
        UUID configId = UUID.randomUUID();
        when(agentConfigUseCase.update(eq(COMPANY_ID), any(AgentConfigRequest.class)))
                .thenReturn(new AgentConfigResponse(configId, true, true, "Você responde como Léo.",
                        "gpt-4o", 0.7, 300, 45, 900, null));

        mockMvc.perform(put("/api/v1/ai/agent-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aiEnabled\":true,\"allowAutoReply\":true,"
                                + "\"systemPrompt\":\"Você responde como Léo.\",\"model\":\"gpt-4o\","
                                + "\"temperature\":0.7,\"maxTokens\":300,"
                                + "\"cooldownMinutes\":45,\"maxChars\":900}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(configId.toString()))
                .andExpect(jsonPath("$.aiEnabled").value(true))
                .andExpect(jsonPath("$.model").value("gpt-4o"));
    }

    @Test
    void shouldRejectInvalidPayload() throws Exception {
        login(List.of("ai:agent-config"));

        mockMvc.perform(put("/api/v1/ai/agent-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aiEnabled\":true,\"allowAutoReply\":false,"
                                + "\"cooldownMinutes\":99999,\"maxChars\":99999}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.errors").isArray());

        verify(agentConfigUseCase, never()).update(any(), any());
    }

    @Test
    void shouldDenyWhenNoCompany() throws Exception {
        loginWithoutCompany(List.of("ai:agent-config"));

        mockMvc.perform(get("/api/v1/ai/agent-config"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CRM_ACCESS_DENIED"));
    }
}