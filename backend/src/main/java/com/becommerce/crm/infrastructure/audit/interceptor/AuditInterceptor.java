package com.becommerce.crm.infrastructure.audit.interceptor;

import com.becommerce.crm.infrastructure.audit.context.AuditContext;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class AuditInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        UUID userId = null;
        UUID companyId = null;

        if (authentication != null && authentication.getPrincipal() instanceof CurrentUser currentUser) {
            userId = currentUser.userId();
            companyId = currentUser.companyId();
        }

        if (userId != null) {
            AuditContext.set(
                userId,
                null,
                null,
                companyId,
                getClientIpAddress(request),
                request.getHeader("User-Agent")
            );
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                 Object handler, Exception ex) {
        AuditContext.clear();
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
