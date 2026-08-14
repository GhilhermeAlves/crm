package com.becommerce.crm.presentation.rest.customer360;

import com.becommerce.crm.application.customer360.dto.ContactSummaryResponse;
import com.becommerce.crm.application.customer360.dto.Customer360Response;
import com.becommerce.crm.application.customer360.dto.NextActionResponse;
import com.becommerce.crm.application.customer360.dto.OpportunityItemResponse;
import com.becommerce.crm.application.customer360.dto.TaskItemResponse;
import com.becommerce.crm.application.customer360.dto.TimelineEventResponse;
import com.becommerce.crm.application.customer360.service.Customer360Service;
import com.becommerce.crm.domain.pipeline.OpportunityStatus;
import com.becommerce.crm.domain.task.TaskPriority;
import com.becommerce.crm.domain.task.TaskStatus;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import com.becommerce.crm.presentation.rest.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class Customer360ControllerTest {

    @Mock private Customer360Service customer360Service;
    @InjectMocks private Customer360Controller controller;

    private MockMvc mockMvc;
    private final UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
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
                List.of("ADMIN"), List.of("contact:read"),
                "k", null, "keycloak", "Admin", null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal, null,
                        principal.permissions().stream().map(SimpleGrantedAuthority::new).toList()));
    }

    private Customer360Response sample(UUID contactId) {
        return new Customer360Response(
                companyId,
                new ContactSummaryResponse(contactId, "Ana Souza", "ana@e.com", "11-99999", null,
                        "AS", LocalDateTime.now(), LocalDateTime.now(), false, null),
                1,
                new BigDecimal("5000.00"),
                List.of(new OpportunityItemResponse(UUID.randomUUID(), "Negócio A", new BigDecimal("5000.00"),
                        "Proposta", 60, OpportunityStatus.OPEN, "ABERTA", "Vendas", null,
                        LocalDateTime.now())),
                List.of(new TaskItemResponse(UUID.randomUUID(), "Ligar", TaskStatus.PENDING,
                        TaskPriority.HIGH, LocalDateTime.now(), null, null, false)),
                List.of(new TimelineEventResponse(UUID.randomUUID(), "ACTIVITY", "Atividade", null,
                        LocalDateTime.now(), contactId, "Chamada")),
                new NextActionResponse("NONE", "Tudo em dia", "Sem urgência.", 0));
    }

    @Test
    void shouldReturnCustomer360ForOwnCompany() throws Exception {
        UUID contactId = UUID.randomUUID();
        login(companyId);
        when(customer360Service.build(companyId, contactId)).thenReturn(sample(contactId));

        mockMvc.perform(get("/api/v1/companies/" + companyId + "/contacts/" + contactId + "/360"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contact.fullName").value("Ana Souza"))
                .andExpect(jsonPath("$.openOpportunities").value(1))
                .andExpect(jsonPath("$.openValue").value(5000.00))
                .andExpect(jsonPath("$.opportunities[0].title").value("Negócio A"))
                .andExpect(jsonPath("$.nextAction.type").value("NONE"));
    }

    @Test
    void shouldReturn403ForOtherCompany() throws Exception {
        UUID otherCompany = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        login(companyId);

        mockMvc.perform(get("/api/v1/companies/" + otherCompany + "/contacts/" + contactId + "/360"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CRM_ACCESS_DENIED"));
    }
}