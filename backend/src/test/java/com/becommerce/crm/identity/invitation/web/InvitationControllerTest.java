package com.becommerce.crm.identity.invitation.web;

import com.becommerce.crm.identity.invitation.application.dto.CreateInvitationRequest;
import com.becommerce.crm.identity.invitation.application.dto.InvitationLinkResponse;
import com.becommerce.crm.identity.invitation.application.dto.InvitationPreviewResponse;
import com.becommerce.crm.identity.invitation.application.dto.InvitationRegisterRequest;
import com.becommerce.crm.identity.invitation.application.dto.InvitationResponse;
import com.becommerce.crm.identity.invitation.application.port.input.InvitationUseCase;
import com.becommerce.crm.identity.invitation.application.service.InvitationSignupService;
import com.becommerce.crm.identity.invitation.domain.InvitationStatus;
import com.becommerce.crm.identity.invitation.domain.exception.InvitationNoLongerValidException;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import com.becommerce.crm.shared.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.becommerce.crm.shared.security.config.CurrentCompanyIdArgumentResolver;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class InvitationControllerTest {

    @Mock private InvitationUseCase invitationUseCase;
    @Mock private InvitationSignupService invitationSignupService;
    @InjectMocks private InvitationController invitationController;

    private MockMvc mockMvc;
    private final UUID companyId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(invitationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new CurrentCompanyIdArgumentResolver(), new AuthenticationPrincipalArgumentResolver())
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
        return new InvitationResponse(UUID.randomUUID(), companyId, "novo@empresa.com", null,
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
    void shouldRejectCrossCompanyAccess() throws Exception {
        // principal pertence à companyId, mas tenta administrar OUTRA empresa
        UUID otherCompany = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/companies/{cid}/invitations", otherCompany)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"novo@empresa.com\",\"role\":\"AGENT\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/companies/{cid}/invitations", otherCompany))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/companies/{cid}/invitations/{iid}", otherCompany, UUID.randomUUID()))
                .andExpect(status().isForbidden());
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
        InvitationResponse r = new InvitationResponse(UUID.randomUUID(), companyId, "novo@empresa.com", null,
                "AGENT", InvitationStatus.REVOKED, userId, LocalDateTime.now().plusDays(7), LocalDateTime.now());
        when(invitationUseCase.decline("tok-abc", userId)).thenReturn(r);
        mockMvc.perform(post("/api/v1/invitations/decline").param("token", "tok-abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"));
    }

    @Test
    void previewIsPublicAndReturnsCompanyEmailAndRole() throws Exception {
        SecurityContextHolder.clearContext();
        when(invitationSignupService.preview(eq("tok-abc"), anyString())).thenReturn(new InvitationPreviewResponse(
                "EmpresaX", "novo@empresa.com", "Fulano", "AGENT", InvitationStatus.PENDING,
                LocalDateTime.now().plusDays(7), false));

        mockMvc.perform(get("/api/v1/invitations/preview").param("token", "tok-abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("EmpresaX"))
                .andExpect(jsonPath("$.email").value("novo@empresa.com"))
                .andExpect(jsonPath("$.role").value("AGENT"))
                .andExpect(jsonPath("$.hasAccount").value(false));
    }

    @Test
    void registerIgnoresAnyEmailSentByTheClient() throws Exception {
        SecurityContextHolder.clearContext();
        InvitationResponse accepted = new InvitationResponse(UUID.randomUUID(), companyId, "novo@empresa.com",
                null, "AGENT", InvitationStatus.ACCEPTED, userId, LocalDateTime.now().plusDays(7),
                LocalDateTime.now());
        when(invitationSignupService.register(any(InvitationRegisterRequest.class), anyString()))
                .thenReturn(accepted);

        mockMvc.perform(post("/api/v1/invitations/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"tok-abc\",\"name\":\"Fulano\",\"password\":\"Kc!Valid1Aa1\","
                                + "\"email\":\"atacante@evil.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("novo@empresa.com"));

        ArgumentCaptor<InvitationRegisterRequest> captor = ArgumentCaptor.forClass(InvitationRegisterRequest.class);
        verify(invitationSignupService).register(captor.capture(), anyString());
        assertEquals("tok-abc", captor.getValue().token());
        assertEquals("Fulano", captor.getValue().name());
    }

    @Test
    void registerWithoutPassword_isBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/invitations/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"tok-abc\",\"name\":\"Fulano\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invitationNoLongerValid_isGone() throws Exception {
        when(invitationUseCase.accept("tok-abc", userId))
                .thenThrow(new InvitationNoLongerValidException(InvitationStatus.ACCEPTED));

        mockMvc.perform(post("/api/v1/invitations/accept").param("token", "tok-abc"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.invitationStatus").value("ACCEPTED"));
    }

    @Test
    void regenerateReturnsNewLink() throws Exception {
        UUID invitationId = UUID.randomUUID();
        when(invitationUseCase.regenerate(companyId, invitationId, false, userId))
                .thenReturn(new InvitationLinkResponse(sample(), "https://crm/convite/novo"));

        mockMvc.perform(post("/api/v1/companies/" + companyId + "/invitations/" + invitationId + "/regenerate")
                        .param("send", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://crm/convite/novo"));
    }

    @Test
    void regenerateForAnotherCompany_isForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/companies/" + UUID.randomUUID() + "/invitations/" + UUID.randomUUID()
                        + "/regenerate").param("send", "true"))
                .andExpect(status().isForbidden());
    }
}