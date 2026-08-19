package com.becommerce.crm.presentation.rest.ai;

import com.becommerce.crm.application.ai.dto.AiConversationResponse;
import com.becommerce.crm.application.ai.dto.AiMessageResponse;
import com.becommerce.crm.application.ai.port.input.AiAssistantUseCase;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AiConversationHistoryControllerTest {

    private static final UUID COMPANY_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID USER_ID = UUID.fromString("974bbedb-298d-4ec6-a037-514b24c248e4");

    @Mock private AiAssistantUseCase aiAssistantUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AiConversationHistoryController controller = new AiConversationHistoryController(aiAssistantUseCase);
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

    @Test
    void shouldListConversations() throws Exception {
        login(List.of("ai:chat"));
        UUID convId = UUID.randomUUID();
        when(aiAssistantUseCase.listConversations(eq(COMPANY_ID), eq(USER_ID)))
                .thenReturn(List.of(new AiConversationResponse(
                        convId, "Como est� o cliente?", "customer360", UUID.randomUUID(),
                        LocalDateTime.now(), LocalDateTime.now())));

        mockMvc.perform(get("/api/v1/ai/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(convId.toString()))
                .andExpect(jsonPath("$[0].title").value("Como est� o cliente?"))
                .andExpect(jsonPath("$[0].screen").value("customer360"));
    }

    @Test
    void shouldListMessagesOfConversation() throws Exception {
        login(List.of("ai:chat"));
        UUID convId = UUID.randomUUID();
        UUID msgId = UUID.randomUUID();
        when(aiAssistantUseCase.getConversationMessages(eq(COMPANY_ID), eq(USER_ID), eq(convId)))
                .thenReturn(List.of(new AiMessageResponse(
                        msgId, convId, "user", "Como est� esse cliente?", LocalDateTime.now())));

        mockMvc.perform(get("/api/v1/ai/conversations/{conversationId}/messages", convId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(msgId.toString()))
                .andExpect(jsonPath("$[0].role").value("user"))
                .andExpect(jsonPath("$[0].content").value("Como est� esse cliente?"));
    }

    @Test
    void shouldDenyWhenNoCompany() throws Exception {
        login(List.of("ai:chat"));
        CurrentUser noCompany = new CurrentUser(
                USER_ID, "admin@empresa.com", null, null,
                List.of(), List.of("ai:chat"),
                "keycloak-sub", null, "keycloak", "Admin", null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        noCompany, null,
                        noCompany.permissions().stream().map(SimpleGrantedAuthority::new).toList()));

        mockMvc.perform(get("/api/v1/ai/conversations"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CRM_ACCESS_DENIED"));
    }
}