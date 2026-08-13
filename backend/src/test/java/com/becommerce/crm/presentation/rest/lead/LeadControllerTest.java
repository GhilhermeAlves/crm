package com.becommerce.crm.presentation.rest.lead;

import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.application.lead.dto.CreateLeadRequest;
import com.becommerce.crm.application.lead.dto.LeadResponse;
import com.becommerce.crm.application.lead.dto.UpdateLeadRequest;
import com.becommerce.crm.application.lead.port.input.LeadUseCase;
import com.becommerce.crm.domain.lead.LeadClassification;
import com.becommerce.crm.domain.lead.LeadSource;
import com.becommerce.crm.domain.lead.LeadStatus;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import com.becommerce.crm.presentation.rest.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LeadControllerTest {

    @Mock private LeadUseCase leadUseCase;
    @InjectMocks private LeadController leadController;

    private MockMvc mockMvc;
    private UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(leadController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void login(UUID activeCompanyId) {
        CurrentUser principal = new CurrentUser(
                UUID.randomUUID(), "admin@empresa.com", activeCompanyId, activeCompanyId,
                List.of("ADMIN"), List.of("lead:create", "lead:read", "lead:update", "lead:delete"),
                "k", null, "keycloak", "Admin", null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal, null,
                        principal.permissions().stream().map(SimpleGrantedAuthority::new).toList()));
    }

    @Test
    void shouldListLeadsOfOwnCompany() throws Exception {
        login(companyId);
        UUID contactId = UUID.randomUUID();
        when(leadUseCase.list(eq(companyId), any(), any(), any(), any(Integer.class),
                any(Integer.class), any(), any()))
                .thenReturn(PageResponse.of(List.of(
                        new LeadResponse(UUID.randomUUID(), companyId, contactId, LeadStatus.NEW,
                                0, null, LeadSource.MANUAL, null, null, null,
                                LocalDateTime.now(), LocalDateTime.now())), 0, 10, 1));

        mockMvc.perform(get("/api/v1/companies/" + companyId + "/leads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldReturn403WhenListingLeadsOfOtherCompany() throws Exception {
        login(companyId);
        mockMvc.perform(get("/api/v1/companies/" + UUID.randomUUID() + "/leads"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CRM_ACCESS_DENIED"));
    }

    @Test
    void shouldCreateLeadInOwnCompany() throws Exception {
        login(companyId);
        UUID contactId = UUID.randomUUID();
        when(leadUseCase.create(any(), any(CreateLeadRequest.class), any(UUID.class)))
                .thenReturn(new LeadResponse(UUID.randomUUID(), companyId, contactId, LeadStatus.NEW,
                        0, null, LeadSource.MANUAL, null, null, null,
                        LocalDateTime.now(), LocalDateTime.now()));

        mockMvc.perform(post("/api/v1/companies/" + companyId + "/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contactId\":\"" + contactId + "\",\"source\":\"MANUAL\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void shouldReturn400WhenCreatingLeadWithoutSource() throws Exception {
        login(companyId);
        UUID contactId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/companies/" + companyId + "/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contactId\":\"" + contactId + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn403WhenCreatingLeadForOtherCompany() throws Exception {
        UUID otherCompany = UUID.randomUUID();
        login(companyId);

        mockMvc.perform(post("/api/v1/companies/" + otherCompany + "/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contactId\":\"" + UUID.randomUUID() + "\",\"source\":\"MANUAL\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CRM_ACCESS_DENIED"));
    }

    @Test
    void shouldUpdateLeadInOwnCompany() throws Exception {
        login(companyId);
        UUID leadId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        when(leadUseCase.update(any(), any(), any(UpdateLeadRequest.class)))
                .thenReturn(new LeadResponse(leadId, companyId, contactId, LeadStatus.CONTACTED,
                        50, LeadClassification.WARM, LeadSource.FORM, null, null, null,
                        LocalDateTime.now(), LocalDateTime.now()));

        mockMvc.perform(put("/api/v1/companies/" + companyId + "/leads/" + leadId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONTACTED\",\"score\":50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONTACTED"));
    }

    @Test
    void shouldReturn403WhenUpdatingLeadForOtherCompany() throws Exception {
        UUID otherCompany = UUID.randomUUID();
        login(companyId);

        mockMvc.perform(put("/api/v1/companies/" + otherCompany + "/leads/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONTACTED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CRM_ACCESS_DENIED"));
    }

    @Test
    void shouldDeleteLeadInOwnCompany() throws Exception {
        login(companyId);
        UUID leadId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/companies/" + companyId + "/leads/" + leadId))
                .andExpect(status().isNoContent());
        verify(leadUseCase).delete(companyId, leadId);
    }

    @Test
    void shouldReturn403WhenDeletingLeadForOtherCompany() throws Exception {
        UUID otherCompany = UUID.randomUUID();
        login(companyId);

        mockMvc.perform(delete("/api/v1/companies/" + otherCompany + "/leads/" + UUID.randomUUID()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CRM_ACCESS_DENIED"));
    }
}