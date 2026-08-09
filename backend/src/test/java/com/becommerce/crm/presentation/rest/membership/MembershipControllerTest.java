package com.becommerce.crm.presentation.rest.membership;

import com.becommerce.crm.application.membership.dto.MemberResponse;
import com.becommerce.crm.application.membership.dto.MembershipResponse;
import com.becommerce.crm.application.membership.port.input.MembershipUseCase;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.crm.domain.membership.exception.MembershipNotFoundException;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MembershipControllerTest {

    private static final UUID COMPANY_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID USER_ID = UUID.fromString("974bbedb-298d-4ec6-a037-514b24c248e4");

    @Mock private MembershipUseCase membershipUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MembershipController controller = new MembershipController(membershipUseCase);
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
    void shouldListMembers() throws Exception {
        login(List.of("membership:view"));
        when(membershipUseCase.listMembers(eq(COMPANY_ID), eq(COMPANY_ID)))
                .thenReturn(List.of(new MemberResponse(
                        USER_ID, "Ghilherme Santos", "ghilherme007@gmail.com", "AGENT", "ACTIVE", LocalDateTime.now())));

        mockMvc.perform(get("/api/v1/companies/{id}/members", COMPANY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$[0].role").value("AGENT"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void shouldDenyListMembersOfAnotherCompany() throws Exception {
        login(List.of("membership:view"));
        when(membershipUseCase.listMembers(any(UUID.class), eq(COMPANY_ID)))
                .thenThrow(new CrmAccessDeniedException("Acesso a membros desta empresa não permitido."));

        mockMvc.perform(get("/api/v1/companies/{id}/members", UUID.randomUUID()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CRM_ACCESS_DENIED"));
    }

    @Test
    void shouldUpdateMemberRole() throws Exception {
        login(List.of("membership:manage"));
        when(membershipUseCase.updateMemberRole(eq(COMPANY_ID), eq(USER_ID), eq("MANAGER"),
                eq(COMPANY_ID), anyBoolean()))
                .thenReturn(new MemberResponse(
                        USER_ID, "Ghilherme Santos", "ghilherme007@gmail.com", "MANAGER", "ACTIVE", LocalDateTime.now()));

        mockMvc.perform(put("/api/v1/companies/{id}/members/{userId}", COMPANY_ID, USER_ID)
                        .contentType("application/json")
                        .content("{\"role\":\"MANAGER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MANAGER"));
    }

    @Test
    void shouldReturn404WhenMemberNotFound() throws Exception {
        login(List.of("membership:manage"));
        when(membershipUseCase.updateMemberRole(eq(COMPANY_ID), eq(USER_ID), eq("MANAGER"),
                eq(COMPANY_ID), anyBoolean()))
                .thenThrow(new MembershipNotFoundException("Membro não encontrado nesta empresa: " + USER_ID));

        mockMvc.perform(put("/api/v1/companies/{id}/members/{userId}", COMPANY_ID, USER_ID)
                        .contentType("application/json")
                        .content("{\"role\":\"MANAGER\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRemoveMember() throws Exception {
        login(List.of("membership:manage"));

        mockMvc.perform(delete("/api/v1/companies/{id}/members/{userId}", COMPANY_ID, USER_ID))
                .andExpect(status().isNoContent());
        verify(membershipUseCase).removeMember(eq(COMPANY_ID), eq(USER_ID), eq(COMPANY_ID), anyBoolean());
    }

    @Test
    void shouldListMyMemberships() throws Exception {
        login(List.of());
        when(membershipUseCase.listMyMemberships(USER_ID))
                .thenReturn(List.of(new MembershipResponse(
                        COMPANY_ID, "Empresa", "ADMIN", "ACTIVE", LocalDateTime.now())));

        mockMvc.perform(get("/api/v1/me/memberships"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].companyId").value(COMPANY_ID.toString()))
                .andExpect(jsonPath("$[0].role").value("ADMIN"));
    }
}
