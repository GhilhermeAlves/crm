package com.becommerce.crm.presentation.rest.invitation;

import com.becommerce.crm.application.invitation.dto.CreateInvitationRequest;
import com.becommerce.crm.application.invitation.dto.InvitationResponse;
import com.becommerce.crm.application.invitation.port.input.InvitationUseCase;
import com.becommerce.crm.domain.invitation.InvitationStatus;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import com.becommerce.crm.presentation.rest.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class InvitationControllerTest {

    @Mock private InvitationUseCase invitationUseCase;
    @InjectMocks private InvitationController invitationController;

    private MockMvc mockMvc;
    private final UUID companyId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(invitationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        login();
    }

    private void login() {
        CurrentUser principal = new CurrentUser(
                userId, "admin@empresa.com", companyId, companyId,
                List.of("ADMIN"),
                List.of("membership:view", "membership:manage"),
                "keycloak-sub", null, "keycloak", "Admin", "ADMIN");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal, null,
                        principal.permissions().stream().map(SimpleGrantedAuthority::new).toList()));
    }

    private InvitationResponse sample() {
        return new InvitationResponse(UUID.randomUUID(), companyId, "novo@empresa.com",
                "AGENT", InvitationStatus.PENDING, userId, LocalDateTime.now().plusDays(7), LocalDateTime.now());
    }

    @Test
    void shouldCreateInvitation() throws Exception {
        InvitationResponse r = sample();
        when(invitationUseCase.create(eq(companyId), any(CreateInvitationRequest.class), eq(userId))).thenReturn(r);

        mockMvc.perform(post("/api/v1/companies/{cid}/invitations", companyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"novo@empresa.com\",\"role\":\"AGENT\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("novo@empresa.com"));
    }

    @Test
    void shouldListInvitations() throws Exception {
        when(invitationUseCase.listByCompany(companyId, null)).thenReturn(List.of(sample()));
        mockMvc.perform(get("/api/v1/companies/{cid}/invitations", companyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("AGENT"));
    }

    @Test
    void shouldRevokeInvitation() throws Exception {
        UUID invitationId = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/companies/{cid}/invitations/{iid}", companyId, invitationId))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldAcceptInvitation() throws Exception {
        InvitationResponse r = sample();
        when(invitationUseCase.accept("tok-abc", userId)).thenReturn(r);
        mockMvc.perform(post("/api/v1/invitations/accept").param("token", "tok-abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldDeclineInvitation() throws Exception {
        InvitationResponse r = new InvitationResponse(UUID.randomUUID(), companyId, "novo@empresa.com",
                "AGENT", InvitationStatus.REVOKED, userId, LocalDateTime.now().plusDays(7), LocalDateTime.now());
        when(invitationUseCase.decline("tok-abc", userId)).thenReturn(r);
        mockMvc.perform(post("/api/v1/invitations/decline").param("token", "tok-abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"));
    }
}