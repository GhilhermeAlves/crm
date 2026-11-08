package com.becommerce.crm.billing.web;

import com.becommerce.crm.billing.application.BillingAccessService;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.List;

/**
 * Empresa com cobrança BLOCKED recebe 402 (BILLING_BLOCKED) em tudo, menos cobrança, identidade e rotas
 * públicas — assim a tela de pagamento pendente continua funcionando. CRM_ADMIN passa sempre.
 */
@Component
public class BillingGuardInterceptor implements HandlerInterceptor {

    private static final List<String> OPEN_EXACT = List.of("/api/v1/billing", "/api/v1/me");
    private static final List<String> OPEN_PREFIXES = List.of(
            "/api/v1/billing/", "/api/v1/me/", "/api/v1/auth/", "/api/v1/public/", "/api/v1/admin/billing/");

    private final BillingAccessService access;

    public BillingGuardInterceptor(BillingAccessService access) { this.access = access; }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CurrentUser user)) return true;
        if (user.isCrmAdmin()) return true;
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (isOpen(request.getMethod(), path)) return true;
        if (user.companyId() == null) return true;
        if (access.isBlocked(user.companyId())) throw new BillingBlockedException();
        return true;
    }

    static boolean isOpen(String method, String path) {
        if ("GET".equals(method) && "/api/v1/companies/me".equals(path)) return true;
        return OPEN_EXACT.contains(path) || OPEN_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
