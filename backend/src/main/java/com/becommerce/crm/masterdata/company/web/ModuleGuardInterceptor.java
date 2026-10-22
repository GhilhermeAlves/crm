package com.becommerce.crm.masterdata.company.web;

import com.becommerce.crm.masterdata.company.application.service.CompanyModulesService;
import com.becommerce.crm.masterdata.company.domain.CompanyModule;
import com.becommerce.crm.masterdata.company.domain.ModuleDisabledException;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recusa com 403 ({@code MODULE_DISABLED}) as rotas de um módulo desligado para
 * a empresa (V104). Pacientes não exige CRM: {@code contacts} é armazenamento
 * compartilhado (inbox, agenda, IA) e por isso não é bloqueado; o comercial é
 * protegido pelas rotas de funil/oportunidades/leads. SUPER_ADMIN passa sempre.
 */
@Component
public class ModuleGuardInterceptor implements HandlerInterceptor {

    private static final Pattern COMPANY_PATH =
            Pattern.compile("^/api/v1/companies/([0-9a-fA-F-]{36})/([a-z-]+)(/.*)?$");
    private static final Pattern MAKE_PATIENT = Pattern.compile("^/[^/]+/make-patient/?$");

    private static final Map<String, CompanyModule> RESOURCE_MODULES = Map.ofEntries(
            Map.entry("patients", CompanyModule.PATIENTS),
            Map.entry("opportunities", CompanyModule.CRM),
            Map.entry("pipelines", CompanyModule.CRM),
            Map.entry("leads", CompanyModule.CRM),
            Map.entry("appointments", CompanyModule.AGENDA),
            Map.entry("appointment-types", CompanyModule.AGENDA),
            Map.entry("blocks", CompanyModule.AGENDA),
            Map.entry("agenda-settings", CompanyModule.AGENDA),
            Map.entry("chairs", CompanyModule.AGENDA),
            Map.entry("recalls", CompanyModule.AGENDA),
            Map.entry("holidays", CompanyModule.AGENDA),
            Map.entry("finance", CompanyModule.FINANCE));

    private final CompanyModulesService modulesService;

    public ModuleGuardInterceptor(CompanyModulesService modulesService) {
        this.modulesService = modulesService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        Matcher matcher = COMPANY_PATH.matcher(path);
        if (!matcher.matches()) {
            return true;
        }
        Optional<CompanyModule> required = requiredModule(matcher.group(2), matcher.group(3));
        if (required.isEmpty() || isSuperAdmin()) {
            return true;
        }
        UUID companyId;
        try {
            companyId = UUID.fromString(matcher.group(1));
        } catch (IllegalArgumentException e) {
            return true;
        }
        if (!modulesService.isEnabled(companyId, required.get())) {
            throw new ModuleDisabledException(required.get());
        }
        return true;
    }

    static Optional<CompanyModule> requiredModule(String resource, String rest) {
        if ("contacts".equals(resource)) {
            return rest != null && MAKE_PATIENT.matcher(rest).matches()
                    ? Optional.of(CompanyModule.PATIENTS)
                    : Optional.empty();
        }
        return Optional.ofNullable(RESOURCE_MODULES.get(resource));
    }

    private static boolean isSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof CurrentUser user
                && user.roles().contains("SUPER_ADMIN");
    }
}
