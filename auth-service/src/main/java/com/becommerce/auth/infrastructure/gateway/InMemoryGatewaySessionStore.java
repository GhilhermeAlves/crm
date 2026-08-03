package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.GatewaySession;
import com.becommerce.auth.domain.gateway.SessionLookup;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Store em memória das sessões de browser do Access Gateway (Sprints 6.1/6.2).
 * Ativada por padrão ({@code auth.gateway.session-store=memory}) e adequada a um
 * nó único — a implementação distribuída é {@link RedisGatewaySessionStore}.
 *
 * <p>Ciclo de vida (6.2):
 * <ul>
 *   <li>{@link #findByToken(String)} retorna {@link SessionLookup} distinguindo
 *       {@code ACTIVE}/{@code EXPIRED}/{@code REVOKED}/{@code NOT_FOUND};</li>
 *   <li>expiração efetiva = min(TTL absoluto, lastAccessedAt + idle timeout) —
 *       o acesso a uma sessão ativa renova {@code lastAccessedAt};</li>
 *   <li>{@link #revoke(String)} deixa um <b>tombstone</b> (sessão marcada com
 *       {@code revokedAt}) purgado após uma retenção curta;</li>
 *   <li>lock por sessão ({@link #lockFor(String)}) com {@link ReentrantLock}
 *       serializa a rotação de tokens no refresh — nunca um lock global.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "auth.gateway.session-store", havingValue = "memory", matchIfMissing = true)
public class InMemoryGatewaySessionStore implements GatewaySessionStore {

    private static final Duration TOMBSTONE_RETENTION = Duration.ofMinutes(5);

    private final OidcGatewayProperties properties;
    private final ConcurrentHashMap<String, GatewaySession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> refreshLocks = new ConcurrentHashMap<>();

    public InMemoryGatewaySessionStore(OidcGatewayProperties properties) {
        this.properties = properties;
    }

    @Override
    public void put(GatewaySession session) {
        sessions.put(session.sessionToken(), session);
    }

    @Override
    public Optional<GatewaySession> get(String sessionToken) {
        SessionLookup lookup = findByToken(sessionToken);
        return lookup.isActive() ? Optional.of(lookup.session()) : Optional.empty();
    }

    @Override
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

    @Override
    public void revoke(String sessionToken) {
        if (sessionToken == null) {
            return;
        }
        sessions.computeIfPresent(sessionToken, (token, session) ->
                session.isRevoked() ? session : session.withRevokedAt(Instant.now()));
    }

    @Override
    public void remove(String sessionToken) {
        if (sessionToken != null) {
            sessions.remove(sessionToken);
        }
    }

    @Override
    public GatewaySessionLock lockFor(String sessionToken) {
        ReentrantLock lock = refreshLocks.computeIfAbsent(sessionToken, ignored -> new ReentrantLock());
        lock.lock();
        return lock::unlock;
    }

    @Scheduled(fixedDelay = 60000)
    @Override
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

    @Override
    public int size() {
        return sessions.size();
    }

    private Duration idleTimeout() {
        return properties.getSessionIdleTimeout();
    }
}
