package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.SessionLookup;
import org.springframework.stereotype.Component;

/**
 * Resolve a sessão de gateway a partir do {@code sessionToken} do cookie
 * (Sprint 6.2). Delega ao {@link GatewaySessionStore}, que aplica expiração
 * absoluta/idle e renova {@code lastAccessedAt} nos acessos ativos.
 */
@Component
public class GatewaySessionResolver {

    private final GatewaySessionStore sessionStore;

    public GatewaySessionResolver(GatewaySessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    public SessionLookup resolve(String sessionToken) {
        return sessionStore.findByToken(sessionToken);
    }
}
