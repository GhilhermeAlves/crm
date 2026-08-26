package com.becommerce.crm.infrastructure.security.authorization;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Leitor das autoridades do usuário autenticado (Sprint 20 — Fase 1).
 * As authorities já incluem as permissões granulares vindas do banco
 * (união dos perfis do usuário), reconstruídas a cada requisição pelo
 * {@code KeycloakJwtAuthenticationConverter}.
 *
 * <p>Usado para enforcement de granularidade fina (ex.: edição de campo)
 * dentro da camada de application, onde {@code @PreAuthorize} não se aplica.
 */
@Component
public class CurrentUserAuthorities {

    public boolean has(String permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(granted -> permission.equals(granted.getAuthority()));
    }
}
