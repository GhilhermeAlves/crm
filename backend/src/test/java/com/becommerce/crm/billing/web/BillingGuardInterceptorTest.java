package com.becommerce.crm.billing.web;

import com.becommerce.crm.billing.application.BillingAccessService;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BillingGuardInterceptorTest {
    private final UUID company = UUID.randomUUID();
    private final BillingAccessService access = mock(BillingAccessService.class);
    private final BillingGuardInterceptor guard = new BillingGuardInterceptor(access);

    private void loginAs(String role) {
        CurrentUser u = new CurrentUser(UUID.randomUUID(), "x@y.com", company, company,
                List.of(role), List.of(), "sub", null, "keycloak", "X", role);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u, null, List.of()));
    }

    private boolean call(String method, String uri) throws Exception {
        return guard.preHandle(new MockHttpServletRequest(method, uri), new MockHttpServletResponse(), new Object());
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void blockedCompanyGets402OnBusinessRoutes() {
        loginAs("ADMIN");
        when(access.isBlocked(company)).thenReturn(true);
        assertThrows(BillingBlockedException.class,
                () -> call("GET", "/api/v1/companies/" + company + "/patients"));
    }

    @Test
    void billingAndIdentityRoutesPass() throws Exception {
        loginAs("ADMIN");
        when(access.isBlocked(company)).thenReturn(true);
        assertTrue(call("GET", "/api/v1/billing"));
        assertTrue(call("POST", "/api/v1/billing/check-payment"));
        assertTrue(call("GET", "/api/v1/me"));
        assertTrue(call("GET", "/api/v1/auth/me"));
        assertTrue(call("GET", "/api/v1/companies/me"));
        assertTrue(call("GET", "/api/v1/me/memberships"));
    }

    @Test
    void similarPrefixesAreNotOpen() {
        loginAs("ADMIN");
        when(access.isBlocked(company)).thenReturn(true);
        assertThrows(BillingBlockedException.class, () -> call("GET", "/api/v1/messages"));
        assertThrows(BillingBlockedException.class, () -> call("GET", "/api/v1/billing-report"));
    }

    @Test
    void crmAdminAlwaysPasses() throws Exception {
        loginAs("SUPER_ADMIN");
        assertTrue(call("GET", "/api/v1/companies/" + company + "/patients"));
        verifyNoInteractions(access);
    }

    @Test
    void activeCompanyPasses() throws Exception {
        loginAs("ADMIN");
        when(access.isBlocked(company)).thenReturn(false);
        assertTrue(call("GET", "/api/v1/companies/" + company + "/patients"));
    }

    @Test
    void anonymousPasses() throws Exception {
        assertTrue(call("POST", "/api/v1/public/billing/asaas/webhook"));
        verifyNoInteractions(access);
    }
}
