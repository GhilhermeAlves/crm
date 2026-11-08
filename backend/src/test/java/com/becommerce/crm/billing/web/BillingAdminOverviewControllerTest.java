package com.becommerce.crm.billing.web;

import com.becommerce.crm.billing.application.BillingOverviewService;
import com.becommerce.crm.billing.application.BillingOverviewService.CompanyBillingStatus;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import com.becommerce.crm.shared.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BillingAdminOverviewControllerTest {
    private final BillingOverviewService service = mock(BillingOverviewService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new BillingAdminOverviewController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();

    private void loginAs(String role) {
        UUID company = UUID.randomUUID();
        CurrentUser u = new CurrentUser(UUID.randomUUID(), "x@y.com", company, company,
                List.of(role), List.of(), "sub", null, "keycloak", "X", role);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u, null, List.of()));
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void crmAdminListsStatuses() throws Exception {
        loginAs("CRM_ADMIN");
        UUID id = UUID.randomUUID();
        when(service.statuses()).thenReturn(List.of(new CompanyBillingStatus(id, "PAST_DUE")));
        mvc.perform(get("/api/v1/admin/billing/companies"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].companyId").value(id.toString()))
           .andExpect(jsonPath("$[0].status").value("PAST_DUE"));
    }

    @Test
    void nonAdminForbidden() throws Exception {
        loginAs("ADMIN");
        mvc.perform(get("/api/v1/admin/billing/companies")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
