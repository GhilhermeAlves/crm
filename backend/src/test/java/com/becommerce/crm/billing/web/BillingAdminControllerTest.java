package com.becommerce.crm.billing.web;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.billing.application.BillingRuleException;
import com.becommerce.crm.billing.application.BillingService;
import com.becommerce.crm.billing.application.BillingService.BillingView;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import com.becommerce.crm.shared.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BillingAdminControllerTest {
    private static final UUID TARGET = UUID.randomUUID();
    private static final String URL = "/api/v1/admin/billing/companies/" + TARGET;
    private final BillingService service = mock(BillingService.class);
    private final TenantAuditRecorder auditor = mock(TenantAuditRecorder.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new BillingAdminController(service, auditor))
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
    void crmAdminActivates() throws Exception {
        loginAs("CRM_ADMIN");
        when(service.activate(TARGET)).thenReturn(
                new BillingView("ACTIVE", "STARTER", 19700, LocalDate.of(2026, 11, 15), null, true));
        mvc.perform(post(URL + "/activate"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void crmAdminExemptsAndIsAudited() throws Exception {
        loginAs("CRM_ADMIN");
        when(service.markExempt(TARGET)).thenReturn(new BillingView("EXEMPT", "STARTER", 19700, null, null, false));
        mvc.perform(post(URL + "/exempt"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("EXEMPT"));
        verify(auditor).record(eq(TARGET), eq(AuditAction.UPDATE), eq(AuditModule.SETTINGS), eq("Billing"),
                eq(TARGET.toString()), anyString(), any(), any());
    }

    @Test
    void nonAdminIsForbidden() throws Exception {
        loginAs("ADMIN");
        mvc.perform(post(URL + "/activate")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void ruleErrorIs400() throws Exception {
        loginAs("CRM_ADMIN");
        when(service.activate(TARGET)).thenThrow(new BillingRuleException("Cadastre o CPF ou CNPJ"));
        mvc.perform(post(URL + "/activate")).andExpect(status().isBadRequest());
    }

    @Test
    void changePrice() throws Exception {
        loginAs("CRM_ADMIN");
        when(service.changePrice(TARGET, 17000)).thenReturn(
                new BillingView("ACTIVE", "STARTER", 17000, null, null, true));
        mvc.perform(put(URL + "/price").contentType("application/json").content("{\"monthlyPriceCents\":17000}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.monthlyPriceCents").value(17000));
    }

    @Test
    void changePriceIsAudited() throws Exception {
        loginAs("CRM_ADMIN");
        when(service.changePrice(TARGET, 17000)).thenReturn(new BillingView("ACTIVE", "STARTER", 17000, null, null, true));
        mvc.perform(put(URL + "/price").contentType("application/json").content("{\"monthlyPriceCents\":17000}"))
           .andExpect(status().isOk());
        verify(auditor).record(eq(TARGET), eq(AuditAction.UPDATE), eq(AuditModule.SETTINGS), eq("Billing"),
                eq(TARGET.toString()), anyString(), any(), any());
    }

    @Test
    void auditRunsUnderTargetTenantAndRestoresContext() throws Exception {
        loginAs("CRM_ADMIN");
        UUID own = UUID.randomUUID();
        TenantContext.setCompanyId(own);
        try {
            AtomicReference<UUID> seen = new AtomicReference<>();
            doAnswer(inv -> { seen.set(TenantContext.getCompanyId()); return null; })
                    .when(auditor).record(any(), any(), any(), any(), any(), any(), any(), any());
            when(service.changePrice(TARGET, 17000)).thenReturn(new BillingView("ACTIVE", "STARTER", 17000, null, null, true));
            mvc.perform(put(URL + "/price").contentType("application/json").content("{\"monthlyPriceCents\":17000}"))
               .andExpect(status().isOk());
            org.junit.jupiter.api.Assertions.assertEquals(TARGET, seen.get());
            org.junit.jupiter.api.Assertions.assertEquals(own, TenantContext.getCompanyId());
        } finally {
            TenantContext.clear();
        }
    }
}
