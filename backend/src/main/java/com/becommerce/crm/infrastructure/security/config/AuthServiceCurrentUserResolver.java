package com.becommerce.crm.infrastructure.security.config;

import com.becommerce.crm.infrastructure.identity.client.AuthServiceClient;
import com.becommerce.crm.infrastructure.identity.client.dto.ResolutionResponse;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Resolução do {@link CurrentUser} via crm-auth-service (camada de identidade).
 * Quando o serviço não resolve a identidade ({@code PROVISIONING_REQUIRED}) ou
 * está indisponível, o provisionamento/resolução local (Sprint 1) é usado como
 * fallback — o sistema nunca fica sem autenticação.
 */
public class AuthServiceCurrentUserResolver implements CurrentUserResolver {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceCurrentUserResolver.class);

    private final AuthServiceClient authServiceClient;
    private final LocalCurrentUserResolver localCurrentUserResolver;

    public AuthServiceCurrentUserResolver(AuthServiceClient authServiceClient,
                                          LocalCurrentUserResolver localCurrentUserResolver) {
        this.authServiceClient = authServiceClient;
        this.localCurrentUserResolver = localCurrentUserResolver;
    }

    @Override
    public CurrentUser resolve(Jwt jwt) {
        try {
            ResolutionResponse response = authServiceClient.currentUser(jwt.getTokenValue());
            if (response != null && response.isResolved()) {
                return response.currentUser().toCurrentUser();
            }
            log.warn("Identity layer sem usuário CRM resolvido (status={}); provisionando localmente",
                    response != null ? response.status() : "null");
            return localCurrentUserResolver.resolve(jwt);
        } catch (Exception e) {
            log.warn("Falha na resolução via auth-service ({}); usando resolução local", e.getMessage());
            return localCurrentUserResolver.resolve(jwt);
        }
    }
}
