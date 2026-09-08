package com.becommerce.crm.presentation.rest.followup;

import com.becommerce.crm.application.followup.dto.FollowUpRequest;
import com.becommerce.crm.application.followup.dto.FollowUpResponse;
import com.becommerce.crm.application.followup.port.input.FollowUpUseCase;
import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.domain.followup.FollowUpAction;
import com.becommerce.crm.domain.followup.FollowUpStatus;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FollowUpControllerTest {

    private static final UUID COMPANY_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID USER_ID = UUID.fromString("974bbedb-298d-4ec6-a037-514b24c248e4");

    @Mock private FollowUpUseCase followUpUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        FollowUpController controller = new FollowUpController(followUpUseCase);
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

    private FollowUpResponse response() {
        return new FollowUpResponse(UUID.randomUUID(), UUID.randomUUID(), FollowUpStatus.PENDING,
                FollowUpAction.SEND_MESSAGE, "Obrigado pelo contato!", LocalDateTime.now().plusHours(1),
                0, null, null, null, null, null, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void shouldCreateFollowUp() throws Exception {
        login(List.of("omnichannel:followup"));
        FollowUpResponse response = response();
        when(followUpUseCase.create(eq(COMPANY_ID), any(FollowUpRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/omnichannel/follow-ups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conversationId":"%s","content":"Obrigado pelo contato!","executeAt":"2030-01-01T10:00:00"}
                                """.formatted(response.conversationId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.actionType").value("SEND_MESSAGE"));
        verify(followUpUseCase).create(eq(COMPANY_ID), any(FollowUpRequest.class));
    }

    @Test
    void shouldListFollowUps() throws Exception {
        login(List.of("omnichannel:followup:read"));
        FollowUpResponse response = response();
        when(followUpUseCase.list(COMPANY_ID, null, 0, 20))
                .thenReturn(PageResponse.of(List.of(response), 0, 20, 1));

        mockMvc.perform(get("/api/v1/omnichannel/follow-ups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldGetFollowUp() throws Exception {
        login(List.of("omnichannel:followup:read"));
        UUID followUpId = UUID.randomUUID();
        FollowUpResponse response = response();
        when(followUpUseCase.get(COMPANY_ID, followUpId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/omnichannel/follow-ups/" + followUpId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.id().toString()));
    }

    @Test
    void shouldCancelFollowUp() throws Exception {
        login(List.of("omnichannel:followup"));
        UUID followUpId = UUID.randomUUID();
        FollowUpResponse cancelled = new FollowUpResponse(followUpId, UUID.randomUUID(),
                FollowUpStatus.CANCELLED, FollowUpAction.SEND_MESSAGE, "Obrigado!", LocalDateTime.now(),
                0, null, null, LocalDateTime.now(), com.becommerce.crm.domain.followup.FollowUpCancellationReason.USER,
                null, LocalDateTime.now(), LocalDateTime.now());
        when(followUpUseCase.cancel(COMPANY_ID, followUpId)).thenReturn(cancelled);

        mockMvc.perform(post("/api/v1/omnichannel/follow-ups/" + followUpId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledReason").value("USER"));
    }
}