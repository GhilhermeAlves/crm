package com.becommerce.crm.presentation.rest.me;

import com.becommerce.crm.application.me.dto.CompanyOptionResponse;
import com.becommerce.crm.application.me.port.input.MeUseCase;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MeControllerTest {

    private static final UUID USER_ID = UUID.fromString("974bbedb-298d-4ec6-a037-514b24c248e4");
    private static final UUID COMPANY_A = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID COMPANY_B = UUID.fromString("bbbbbbbb-1111-2222-3333-444444444444");

    @Mock private MeUseCase meUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MeController controller = new MeController(meUseCase);
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
                USER_ID, "user@empresa.com", COMPANY_A, COMPANY_A,
                List.of("AGENT"), List.of(),
                "keycloak-sub", null, "keycloak", "User", "AGENT");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal, null,
                        principal.permissions().stream().map(SimpleGrantedAuthority::new).toList()));
    }

    @Test
    void shouldListMyCompanies() throws Exception {
        login();
        when(meUseCase.listMyCompanies(USER_ID))
                .thenReturn(List.of(
                        new CompanyOptionResponse(COMPANY_A, "Empresa A", "logo-a.png", true),
                        new CompanyOptionResponse(COMPANY_B, "Empresa B", null, false)));

        mockMvc.perform(get("/api/v1/me/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].companyId").value(COMPANY_A.toString()))
                .andExpect(jsonPath("$[0].name").value("Empresa A"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].companyId").value(COMPANY_B.toString()))
                .andExpect(jsonPath("$[1].active").value(false));
    }

    @Test
    void shouldSwitchCompany() throws Exception {
        login();
        when(meUseCase.switchCompany(eq(USER_ID), eq(COMPANY_B)))
                .thenReturn(new CompanyOptionResponse(COMPANY_B, "Empresa B", null, true));

        mockMvc.perform(post("/api/v1/me/switch-company")
                        .contentType("application/json")
                        .content("{\"companyId\":\"" + COMPANY_B + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyId").value(COMPANY_B.toString()))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldRejectMissingCompanyId() throws Exception {
        login();

        mockMvc.perform(post("/api/v1/me/switch-company")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenNoActiveMembershipOnSwitch() throws Exception {
        login();
        when(meUseCase.switchCompany(eq(USER_ID), eq(COMPANY_B)))
                .thenThrow(new MembershipNotFoundException("Usuário sem membership ativa na empresa: " + COMPANY_B));

        mockMvc.perform(post("/api/v1/me/switch-company")
                        .contentType("application/json")
                        .content("{\"companyId\":\"" + COMPANY_B + "\"}"))
                .andExpect(status().isNotFound());
    }
}