package com.becommerce.crm.presentation.rest.followup;

import com.becommerce.crm.application.followup.dto.FollowUpSequenceRequest;
import com.becommerce.crm.application.followup.dto.FollowUpSequenceResponse;
import com.becommerce.crm.application.followup.port.input.FollowUpSequenceUseCase;
import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.domain.followup.FollowUpSequenceStatus;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FollowUpSequenceControllerTest {

    private static final UUID COMPANY_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID USER_ID = UUID.fromString("974bbedb-298d-4ec6-a037-514b24c248e4");

    @Mock private FollowUpSequenceUseCase sequenceUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        FollowUpSequenceController controller = new FollowUpSequenceController(sequenceUseCase);
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

    private FollowUpSequenceResponse response(FollowUpSequenceStatus status) {
        return new FollowUpSequenceResponse(UUID.randomUUID(), "Carrinho abandonado", "Recuperação",
                status, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void shouldCreateSequence() throws Exception {
        login(List.of("omnichannel:followup:sequence"));
        FollowUpSequenceResponse response = response(FollowUpSequenceStatus.ACTIVE);
        when(sequenceUseCase.create(eq(COMPANY_ID), any(FollowUpSequenceRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/omnichannel/follow-up-sequences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Carrinho abandonado","description":"Recuperação"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.name").value("Carrinho abandonado"));
        verify(sequenceUseCase).create(eq(COMPANY_ID), any(FollowUpSequenceRequest.class));
    }

    @Test
    void shouldListSequences() throws Exception {
        login(List.of("omnichannel:followup:sequence:read"));
        FollowUpSequenceResponse response = response(FollowUpSequenceStatus.ACTIVE);
        when(sequenceUseCase.list(COMPANY_ID, 0, 20))
                .thenReturn(PageResponse.of(List.of(response), 0, 20, 1));

        mockMvc.perform(get("/api/v1/omnichannel/follow-up-sequences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Carrinho abandonado"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldGetSequence() throws Exception {
        login(List.of("omnichannel:followup:sequence:read"));
        UUID sequenceId = UUID.randomUUID();
        FollowUpSequenceResponse response = response(FollowUpSequenceStatus.ACTIVE);
        when(sequenceUseCase.get(COMPANY_ID, sequenceId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/omnichannel/follow-up-sequences/" + sequenceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.id().toString()));
    }

    @Test
    void shouldUpdateSequence() throws Exception {
        login(List.of("omnichannel:followup:sequence"));
        UUID sequenceId = UUID.randomUUID();
        FollowUpSequenceResponse response = response(FollowUpSequenceStatus.ACTIVE);
        when(sequenceUseCase.update(eq(COMPANY_ID), eq(sequenceId), any(FollowUpSequenceRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/omnichannel/follow-up-sequences/" + sequenceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Novo nome","description":"Nova desc"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Carrinho abandonado"));
    }

    @Test
    void shouldDeleteSequence() throws Exception {
        login(List.of("omnichannel:followup:sequence"));
        UUID sequenceId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/omnichannel/follow-up-sequences/" + sequenceId))
                .andExpect(status().isNoContent());
        verify(sequenceUseCase).delete(COMPANY_ID, sequenceId);
    }

    @Test
    void shouldDeactivateSequence() throws Exception {
        login(List.of("omnichannel:followup:sequence"));
        UUID sequenceId = UUID.randomUUID();
        FollowUpSequenceResponse response = response(FollowUpSequenceStatus.INACTIVE);
        when(sequenceUseCase.deactivate(COMPANY_ID, sequenceId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/omnichannel/follow-up-sequences/" + sequenceId + "/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void shouldActivateSequence() throws Exception {
        login(List.of("omnichannel:followup:sequence"));
        UUID sequenceId = UUID.randomUUID();
        FollowUpSequenceResponse response = response(FollowUpSequenceStatus.ACTIVE);
        when(sequenceUseCase.activate(COMPANY_ID, sequenceId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/omnichannel/follow-up-sequences/" + sequenceId + "/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}