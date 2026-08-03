package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.GatewaySession;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Store em memória das sessões de browser do Access Gateway. A sessão é criada
 * somente após CRM Access positivo e é referenciada pelo {@code sessionToken}
 * opaco do cookie HttpOnly. Sessão expirada é invalidada na leitura e purgada
 * periodicamente.
 *
 * <p>Nota de arquitetura: em memória é suficiente para um nó único; a migração
 * para Redis é deixada para sprint posterior.
 */
@Component
public class GatewaySessionStore {

    private final ConcurrentHashMap<String, GatewaySession> sessions = new ConcurrentHashMap<>();

    public void put(GatewaySession session) {
        sessions.put(session.sessionToken(), session);
    }

    public Optional<GatewaySession> get(String sessionToken) {
        if (sessionToken == null) {
            return Optional.empty();
        }
        GatewaySession session = sessions.get(sessionToken);
        if (session == null) {
            return Optional.empty();
        }
        if (session.expiresAt().isBefore(Instant.now())) {
            sessions.remove(sessionToken);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    public void revoke(String sessionToken) {
        if (sessionToken != null) {
            sessions.remove(sessionToken);
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void purgeExpired() {
        Instant now = Instant.now();
        sessions.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    public int size() {
        return sessions.size();
    }
}
