package com.becommerce.crm.billing.web;

import com.becommerce.crm.billing.application.BillingService;
import com.becommerce.crm.billing.application.BillingService.BillingView;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BillingControllerTest {
    private final UUID company = UUID.randomUUID();
    private final BillingService service = mock(BillingService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new BillingController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();

    private void login(UUID companyId) {
        CurrentUser u = new CurrentUser(UUID.randomUUID(), "x@y.com", companyId, companyId,
                List.of("USER"), List.of(), "sub", null, "keycloak", "X", "USER");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u, null, List.of()));
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void viewUsesLoggedCompany() throws Exception {
        login(company);
        when(service.view(company)).thenReturn(
                new BillingView("PAST_DUE", "STARTER", 19700, null, null, true, null, null, "https://pix/1"));
        mvc.perform(get("/api/v1/billing"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.openInvoiceUrl").value("https://pix/1"));
    }

    @Test
    void checkPayment() throws Exception {
        login(company);
        when(service.checkPayment(company)).thenReturn(
                new BillingView("ACTIVE", "STARTER", 19700, null, null, true, null, null, null));
        mvc.perform(post("/api/v1/billing/check-payment"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void withoutCompanyIsForbidden() throws Exception {
        login(null);
        mvc.perform(get("/api/v1/billing")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void hidesInternalSyncError() throws Exception {
        login(company);
        when(service.view(company)).thenReturn(
                new BillingView("SYNC_PENDING", "STARTER", 19700, null, "x", false, null, null, null));
        mvc.perform(get("/api/v1/billing"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.syncError").isEmpty());
    }
}
