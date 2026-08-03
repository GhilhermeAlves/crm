package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.GatewaySession;
import com.becommerce.auth.domain.gateway.SessionLookup;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Store em memória das sessões de browser do Access Gateway (Sprints 6.1/6.2).
 * A sessão é criada somente após CRM Access positivo e referenciada pelo
 * {@code sessionToken} opaco do cookie HttpOnly.
 *
 * <p>Ciclo de vida (6.2):
 * <ul>
 *   <li>{@link #findByToken(String)} retorna {@link SessionLookup} distinguindo
 *       {@code ACTIVE}/{@code EXPIRED}/{@code REVOKED}/{@code NOT_FOUND};</li>
 *   <li>expiração efetiva = min(TTL absoluto, lastAccessedAt + idle timeout) —
 *       o acesso a uma sessão ativa renova {@code lastAccessedAt};</li>
 *   <li>{@link #revoke(String)} deixa um <b>tombstone</b> (sessão marcada com
 *       {@code revokedAt}) para que replays do cookie antigo retornem
 *       {@code REVOKED} em vez de {@code NOT_FOUND}; tombstones são purgados após
 *       uma retenção curta;</li>
 *   <li>lock por sessão ({@link #lockFor(String)}) serializa a rotação de tokens
 *       no refresh — nunca há um lock global.</li>
 * </ul>
 *
 * <p>Nota de arquitetura: em memória é suficiente para um nó único; a migração
 * para Redis é deixada para sprint posterior.
 */
@Component
public class GatewaySessionStore {

    private static final Duration TOMBSTONE_RETENTION = Duration.ofMinutes(5);

    private final OidcGatewayProperties properties;
    private final ConcurrentHashMap<String, GatewaySession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> refreshLocks = new ConcurrentHashMap<>();

    public GatewaySessionStore(OidcGatewayProperties properties) {
        this.properties = properties;
    }

    public void put(GatewaySession session) {
        sessions.put(session.sessionToken(), session);
    }

    public Optional<GatewaySession> get(String sessionToken) {
        SessionLookup lookup = findByToken(sessionToken);
        return lookup.isActive() ? Optional.of(lookup.session()) : Optional.empty();
    }

    public SessionLookup findByToken(String sessionToken) {
        if (sessionToken == null) {
            return SessionLookup.notFound();
        }
        Instant now = Instant.now();
        GatewaySession session = sessions.get(sessionToken);
        if (session == null) {
            return SessionLookup.notFound();
        }
        if (session.isRevoked()) {
            return SessionLookup.revoked(session);
        }
        if (!session.isActive(now, idleTimeout())) {
            sessions.remove(sessionToken, session);
            return SessionLookup.expired(session);
        }
        GatewaySession touched = session.withLastAccessed(now);
        sessions.replace(sessionToken, session, touched);
        return SessionLookup.active(touched);
    }

    /**
     * Revoga a sessão deixando um tombstone (idempotente). Um replay do cookie
     * antigo passa a resolver {@code REVOKED}.
     */
    public void revoke(String sessionToken) {
        if (sessionToken == null) {
            return;
        }
        sessions.computeIfPresent(sessionToken, (token, session) ->
                session.isRevoked() ? session : session.withRevokedAt(Instant.now()));
    }

    public void remove(String sessionToken) {
        if (sessionToken != null) {
            sessions.remove(sessionToken);
        }
    }

    /**
     * Lock por sessão para serializar a rotação de tokens no refresh. O lock é
     * removido pela purga quando a sessão deixa de existir.
     */
    public Object lockFor(String sessionToken) {
        return refreshLocks.computeIfAbsent(sessionToken, ignored -> new Object());
    }

    @Scheduled(fixedDelay = 60000)
    public void purgeExpired() {
        Instant now = Instant.now();
        sessions.entrySet().removeIf(entry -> {
            GatewaySession session = entry.getValue();
            if (session.isRevoked()) {
                return now.isAfter(session.revokedAt().plus(TOMBSTONE_RETENTION));
            }
            return !session.isActive(now, idleTimeout());
        });
        refreshLocks.keySet().removeIf(token -> !sessions.containsKey(token));
    }

    public int size() {
        return sessions.size();
    }

    private Duration idleTimeout() {
        return properties.getSessionIdleTimeout();
    }
}
