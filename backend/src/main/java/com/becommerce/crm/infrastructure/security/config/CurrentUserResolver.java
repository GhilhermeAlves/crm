package com.becommerce.crm.infrastructure.security.config;

import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Resolve o {@link CurrentUser} de um JWT do Keycloak já validado pelo resource
 * server. A implementação concreta é selecionada pela flag
 * {@code app.auth.identity-layer.enabled}: local (banco CRM) ou via
 * crm-auth-service.
 */
public interface CurrentUserResolver {

    CurrentUser resolve(Jwt jwt);
}
