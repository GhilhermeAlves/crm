package com.becommerce.crm.presentation.rest.ai;

import com.becommerce.crm.application.ai.dto.AiActionResponse;
import com.becommerce.crm.application.ai.port.input.AiActionUseCase;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import com.becommerce.crm.presentation.rest.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AiActionControllerTest {

    private static final UUID COMPANY_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID USER_ID = UUID.fromString("974bbedb-298d-4ec6-a037-514b24c248e4");

    @Mock private AiActionUseCase aiActionUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AiActionController controller = new AiActionController(aiActionUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void login() {
        CurrentUser principal = new CurrentUser(
                USER_ID, "admin@empresa.com", COMPANY_ID, COMPANY_ID,
                List.of("ADMIN"), List.of("ai:chat"),
                "keycloak-sub", null, "keycloak", "Admin", "ADMIN");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal, null,
                        principal.permissions().stream().map(SimpleGrantedAuthority::new).toList()));
    }

    private AiActionResponse sample(UUID id, String status) {
        return new AiActionResponse(id, UUID.randomUUID(), "create_task", "TASK", null,
                "Criar tarefa: Ligar", status, Map.of("title", "Ligar"), null, null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void shouldConfirmAction() throws Exception {
        login();
        UUID actionId = UUID.randomUUID();
        when(aiActionUseCase.confirm(eq(COMPANY_ID), eq(USER_ID), eq(List.of("ai:chat")), eq(actionId)))
                .thenReturn(sample(actionId, "EXECUTED"));

        mockMvc.perform(post("/api/v1/ai/actions/{actionId}/confirm", actionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(actionId.toString()))
                .andExpect(jsonPath("$.status").value("EXECUTED"));
    }

    @Test
    void shouldCancelAction() throws Exception {
        login();
        UUID actionId = UUID.randomUUID();
        when(aiActionUseCase.cancel(eq(COMPANY_ID), eq(USER_ID), eq(actionId)))
                .thenReturn(sample(actionId, "CANCELLED"));

        mockMvc.perform(post("/api/v1/ai/actions/{actionId}/cancel", actionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shouldListActionsByConversation() throws Exception {
        login();
        UUID conversationId = UUID.randomUUID();
        UUID actionId = UUID.randomUUID();
        when(aiActionUseCase.listByConversation(eq(COMPANY_ID), eq(USER_ID), eq(conversationId)))
                .thenReturn(List.of(sample(actionId, "PROPOSED")));

        mockMvc.perform(get("/api/v1/ai/conversations/{conversationId}/actions", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(actionId.toString()))
                .andExpect(jsonPath("$[0].status").value("PROPOSED"));
    }

    @Test
    void shouldDenyWhenNoCompany() throws Exception {
        CurrentUser noCompany = new CurrentUser(
                USER_ID, "admin@empresa.com", null, null,
                List.of(), List.of("ai:chat"),
                "keycloak-sub", null, "keycloak", "Admin", null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        noCompany, null,
                        noCompany.permissions().stream().map(SimpleGrantedAuthority::new).toList()));

        mockMvc.perform(post("/api/v1/ai/actions/{actionId}/confirm", UUID.randomUUID()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CRM_ACCESS_DENIED"));
    }
}