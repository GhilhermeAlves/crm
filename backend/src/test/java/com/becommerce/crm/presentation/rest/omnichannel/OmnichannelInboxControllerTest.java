package com.becommerce.crm.presentation.rest.omnichannel;

import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.application.omnichannel.dto.ConversationDetailResponse;
import com.becommerce.crm.application.omnichannel.dto.ConversationResponse;
import com.becommerce.crm.application.omnichannel.dto.MessageResponse;
import com.becommerce.crm.application.omnichannel.port.input.OmnichannelInboxUseCase;
import com.becommerce.crm.domain.omnichannel.ConversationMode;
import com.becommerce.crm.domain.omnichannel.ConversationStatus;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OmnichannelInboxControllerTest {

    private static final UUID COMPANY_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID USER_ID = UUID.fromString("974bbedb-298d-4ec6-a037-514b24c248e4");

    @Mock private OmnichannelInboxUseCase inboxUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        OmnichannelInboxController controller = new OmnichannelInboxController(inboxUseCase);
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

    private ConversationResponse response(UUID conversationId, ConversationMode mode) {
        return new ConversationResponse(conversationId, UUID.randomUUID(), UUID.randomUUID(),
                "+5511999998888", ConversationStatus.OPEN, mode, LocalDateTime.now(),
                "Bom dia!", 1, LocalDateTime.now());
    }

    @Test
    void shouldListConversations() throws Exception {
        login(List.of("omnichannel:read"));
        UUID convId = UUID.randomUUID();
        when(inboxUseCase.listConversations(COMPANY_ID, 0, 20))
                .thenReturn(PageResponse.of(List.of(response(convId, ConversationMode.AUTOMATIC)), 0, 20, 1));

        mockMvc.perform(get("/api/v1/omnichannel/inbox?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].mode").value("AUTOMATIC"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldGetConversationDetail() throws Exception {
        login(List.of("omnichannel:read"));
        UUID convId = UUID.randomUUID();
        when(inboxUseCase.getConversation(COMPANY_ID, convId, 0, 30))
                .thenReturn(new ConversationDetailResponse(convId, UUID.randomUUID(), UUID.randomUUID(),
                        "+5511999998888", ConversationStatus.OPEN, ConversationMode.AUTOMATIC,
                        LocalDateTime.now(), 1, PageResponse.of(List.of(), 0, 20, 0)));

        mockMvc.perform(get("/api/v1/omnichannel/inbox/" + convId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("AUTOMATIC"));
    }

    @Test
    void shouldTakeoverConversation() throws Exception {
        login(List.of("omnichannel:takeover"));
        UUID convId = UUID.randomUUID();
        when(inboxUseCase.takeover(COMPANY_ID, convId)).thenReturn(response(convId, ConversationMode.HUMAN));

        mockMvc.perform(post("/api/v1/omnichannel/inbox/" + convId + "/takeover"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("HUMAN"));
    }

    @Test
    void shouldReleaseConversation() throws Exception {
        login(List.of("omnichannel:takeover"));
        UUID convId = UUID.randomUUID();
        when(inboxUseCase.release(COMPANY_ID, convId)).thenReturn(response(convId, ConversationMode.AUTOMATIC));

        mockMvc.perform(post("/api/v1/omnichannel/inbox/" + convId + "/release"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("AUTOMATIC"));
    }
}