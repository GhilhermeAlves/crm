package com.becommerce.crm.infrastructure.tenant.filter;

import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(2)
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            resolveTenantContext();
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void resolveTenantContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof CurrentUser currentUser) {
            UUID companyId = currentUser.companyId();
            if (companyId != null) {
                TenantContext.setCompanyId(companyId);
            }
        }
    }
}
