package com.becommerce.crm.masterdata.company.web;

import com.becommerce.crm.masterdata.company.application.service.CompanyModulesService;
import com.becommerce.crm.masterdata.company.domain.CompanyModule;
import com.becommerce.crm.masterdata.company.domain.ModuleDisabledException;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModuleGuardInterceptorTest {

    private final CompanyModulesService modules = mock(CompanyModulesService.class);
    private final ModuleGuardInterceptor interceptor = new ModuleGuardInterceptor(modules);
    private final UUID companyId = UUID.randomUUID();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private boolean call(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        return interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
    }

    private void login(String role) {
        CurrentUser user = new CurrentUser(UUID.randomUUID(), "u@e.com", companyId, companyId,
                List.of(role), List.of(), "k", null, "keycloak", "U", null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @Test
    void shouldMapResourcesToModules() {
        assertEquals(Optional.of(CompanyModule.PATIENTS), ModuleGuardInterceptor.requiredModule("patients", "/x"));
        assertEquals(Optional.of(CompanyModule.PATIENTS),
                ModuleGuardInterceptor.requiredModule("contacts", "/abc/make-patient"));
        assertEquals(Optional.empty(), ModuleGuardInterceptor.requiredModule("contacts", "/abc"));
        assertEquals(Optional.of(CompanyModule.CRM), ModuleGuardInterceptor.requiredModule("opportunities", null));
        assertEquals(Optional.of(CompanyModule.AGENDA), ModuleGuardInterceptor.requiredModule("appointments", null));
        assertEquals(Optional.of(CompanyModule.FINANCE), ModuleGuardInterceptor.requiredModule("finance", "/t"));
        assertEquals(Optional.empty(), ModuleGuardInterceptor.requiredModule("users", null));
    }

    @Test
    void shouldAllowPatientsWhenModuleEnabledEvenWithCrmDisabled() {
        login("ADMIN");
        when(modules.isEnabled(companyId, CompanyModule.PATIENTS)).thenReturn(true);

        assertTrue(call("/api/v1/companies/" + companyId + "/patients/" + UUID.randomUUID() + "/notes"));
        verify(modules, never()).isEnabled(companyId, CompanyModule.CRM);
    }

    @Test
    void shouldRejectDisabledModule() {
        login("ADMIN");
        when(modules.isEnabled(companyId, CompanyModule.PATIENTS)).thenReturn(false);

        ModuleDisabledException ex = assertThrows(ModuleDisabledException.class,
                () -> call("/api/v1/companies/" + companyId + "/patients"));
        assertEquals(CompanyModule.PATIENTS, ex.getModule());
    }

    @Test
    void superAdminShouldAlwaysPass() {
        login("SUPER_ADMIN");

        assertTrue(call("/api/v1/companies/" + companyId + "/finance/transactions"));
        verify(modules, never()).isEnabled(any(), any());
    }

    @Test
    void shouldIgnoreUnguardedRoutes() {
        login("ADMIN");

        assertTrue(call("/api/v1/companies/" + companyId + "/contacts"));
        assertTrue(call("/api/v1/omnichannel/inbox"));
        verify(modules, never()).isEnabled(any(), any());
    }
}
