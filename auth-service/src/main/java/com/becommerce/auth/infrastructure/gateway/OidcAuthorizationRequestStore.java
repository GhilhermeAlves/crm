package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.OidcAuthorizationRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Store em memória dos estados transitórios de autorização OIDC. Acesso
 * <b>single-use</b>: {@link #consume(String)} remove o estado da primeira vez que
 * é usado (anti-replay). Estado expirado é purgado por {@link #purgeExpired()}.
 *
 * <p>Nota de arquitetura: em memória é suficiente para um nó único; a migração
 * para Redis (compartilhado entre réplicas) é deixada para sprint posterior.
 */
@Component
public class OidcAuthorizationRequestStore {

    private final ConcurrentHashMap<String, OidcAuthorizationRequest> requests = new ConcurrentHashMap<>();

    public void put(OidcAuthorizationRequest request) {
        requests.put(request.getState(), request);
    }

    /**
     * Consome o estado atômico. Retorna {@code null} se desconhecido, expirado
     * ou já consumido (replay).
     */
    public OidcAuthorizationRequest consume(String state) {
        if (state == null) {
            return null;
        }
        OidcAuthorizationRequest request = requests.remove(state);
        if (request == null || request.isExpired(Instant.now()) || !request.consume()) {
            return null;
        }
        return request;
    }

    @Scheduled(fixedDelay = 60000)
    public void purgeExpired() {
        Instant now = Instant.now();
        requests.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    public int size() {
        return requests.size();
    }
}
