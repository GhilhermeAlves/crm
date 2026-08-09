package com.becommerce.crm.infrastructure.tenant.filter;

import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantFilterTest {

    private TenantFilter tenantFilter;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        tenantFilter = new TenantFilter();
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void shouldSetTenantContextFromCurrentUser() throws ServletException, IOException {
        UUID companyId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(
                UUID.randomUUID(), "test@test.com", companyId, companyId,
                List.of("ADMIN"), List.of("user:read"), "sub-123", null, "keycloak", "Test", null
        );

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(currentUser);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        tenantFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotSetTenantContextWhenNotAuthenticated() throws ServletException, IOException {
        SecurityContextHolder.clearContext();

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        tenantFilter.doFilterInternal(request, response, filterChain);

        assertFalse(TenantContext.hasCompanyId());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldClearContextInFinally() throws ServletException, IOException {
        UUID companyId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(
                UUID.randomUUID(), "test@test.com", companyId, companyId,
                List.of("ADMIN"), List.of("user:read"), "sub-123", null, "keycloak", "Test", null
        );

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(currentUser);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        tenantFilter.doFilterInternal(request, response, filterChain);

        assertFalse(TenantContext.hasCompanyId());
    }
}
