package com.becommerce.crm.infrastructure.security.config;

import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Resolve o {@code companyId} do path ({@code /api/v1/companies/{companyId}/...})
 * e centraliza a checagem de tenant da request: o valor do path é comparado ao
 * {@code companyId} do usuário autenticado {@link CurrentUser} (a mesma fonte que
 * o {@code TenantFilter} usa para popular o {@code TenantContext}), com bypass para
 * SUPER_ADMIN. Reproduz em um único lugar a antiga checagem
 * {@code requireCompanyAccess}, sem mudança de comportamento: 403
 * {@code CRM_ACCESS_DENIED} com a mensagem por recurso declarada na anotação
 * {@link CurrentCompanyId}.
 */
@Component
public class CurrentCompanyIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentCompanyId.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        if (!parameter.getParameterType().equals(UUID.class)) {
            throw new IllegalStateException(
                    "@CurrentCompanyId só pode marcar parâmetros do tipo UUID");
        }
        CurrentCompanyId annotation = parameter.getParameterAnnotation(CurrentCompanyId.class);
        String message = annotation.value();

        UUID companyId = pathCompanyId(webRequest, message);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication != null && authentication.getPrincipal() instanceof CurrentUser principal)) {
            throw new CrmAccessDeniedException(message);
        }
        boolean superAdmin = principal.roles().contains("SUPER_ADMIN");
        if (!superAdmin && !companyId.equals(principal.companyId())) {
            throw new CrmAccessDeniedException(message);
        }
        return companyId;
    }

    private UUID pathCompanyId(NativeWebRequest webRequest, String message) {
        Object attribute = webRequest.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (!(attribute instanceof Map<?, ?> uriVariables)) {
            throw new CrmAccessDeniedException(message);
        }
        Object rawCompanyId = uriVariables.get("companyId");
        if (!(rawCompanyId instanceof String value)) {
            throw new CrmAccessDeniedException(message);
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new CrmAccessDeniedException(message);
        }
    }
}