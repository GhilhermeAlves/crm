package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.GatewaySession;
import com.becommerce.auth.domain.gateway.OidcGatewayException;
import com.becommerce.auth.domain.gateway.SessionLookup;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Store distribuída das sessões de gateway em Redis (Sprint 6.3). Ativada com
 * {@code auth.gateway.session-store=redis} e compartilhada entre réplicas do
 * auth-service.
 *
 * <p>Modelo de dados (somente o domínio serializável da
 * {@link GatewaySession} — nunca HTTP/SecurityContext/JPA/objetos Spring):
 * <ul>
 *   <li>{@code gateway:session:<sessionToken>} → JSON da sessão;</li>
 *   <li>{@code gateway:refresh-lock:<sessionToken>} → lock distribuído por
 *       sessão (SET NX + TTL curto + release via Lua comparando o owner).</li>
 * </ul>
 *
 * <p>Ciclo de vida preservado da 6.2:
 * <ul>
 *   <li><b>TTL nativo</b> do Redis como piso de expiração (expiração efetiva +
 *       janela de tombstone) e <b>expiração lógica</b> da aplicação na leitura —
 *       uma chave ainda presente além da expiração resolve {@code EXPIRED} (e é
 *       removida), não {@code NOT_FOUND};</li>
 *   <li>{@link #revoke(String)} marca {@code revokedAt} e encurta o TTL para a
 *       retenção de tombstone (replay do cookie antigo → {@code REVOKED});</li>
 *   <li>{@link #lockFor(String)} é distribuído — {@code synchronized} local não
 *       serializa réplicas.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "auth.gateway.session-store", havingValue = "redis")
public class RedisGatewaySessionStore implements GatewaySessionStore {

    private static final Logger log = LoggerFactory.getLogger(RedisGatewaySessionStore.class);

    private static final String SESSION_KEY_PREFIX = "gateway:session:";
    private static final String LOCK_KEY_PREFIX = "gateway:refresh-lock:";
    private static final Duration TOMBSTONE_RETENTION = Duration.ofMinutes(5);
    private static final long LOCK_RETRY_DELAY_MILLIS = 50;

    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>("""
            if redis.call("get", KEYS[1]) == ARGV[1] then
                return redis.call("del", KEYS[1])
            else
                return 0
            end
            """, Long.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final OidcGatewayProperties properties;

    public RedisGatewaySessionStore(StringRedisTemplate redis,
                                    ObjectMapper objectMapper,
                                    OidcGatewayProperties properties) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public void put(GatewaySession session) {
        String key = sessionKey(session.sessionToken());
        redisRun(() -> redis.opsForValue().set(key, serialize(session), redisTtl(session)));
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
        String key = sessionKey(sessionToken);
        Optional<GatewaySession> found = readValue(key);
        if (found.isEmpty()) {
            return SessionLookup.notFound();
        }
        GatewaySession session = found.get();
        if (session.isRevoked()) {
            return SessionLookup.revoked(session);
        }
        Instant now = Instant.now();
        if (!session.isActive(now, idleTimeout())) {
            redisOps(() -> redis.delete(key));
            return SessionLookup.expired(session);
        }
        GatewaySession touched = session.withLastAccessed(now);
        redisRun(() -> redis.opsForValue().set(key, serialize(touched), redisTtl(touched)));
        return SessionLookup.active(touched);
    }

    @Override
    public void revoke(String sessionToken) {
        if (sessionToken == null) {
            return;
        }
        String key = sessionKey(sessionToken);
        Optional<GatewaySession> found = readValue(key);
        if (found.isEmpty() || found.get().isRevoked()) {
            return;
        }
        GatewaySession revoked = found.get().withRevokedAt(Instant.now());
        redisRun(() -> redis.opsForValue().set(key, serialize(revoked), redisTtl(revoked)));
    }

    @Override
    public void remove(String sessionToken) {
        if (sessionToken != null) {
            redisOps(() -> redis.delete(sessionKey(sessionToken)));
        }
    }

    @Override
    public GatewaySessionLock lockFor(String sessionToken) {
        String lockKey = lockKey(sessionToken);
        String owner = UUID.randomUUID().toString();
        Duration lockTtl = properties.getSessionLockTtl();
        long deadlineNanos = System.nanoTime() + properties.getSessionLockAcquireTimeout().toNanos();
        while (true) {
            Boolean acquired = redisOps(() -> redis.opsForValue().setIfAbsent(lockKey, owner, lockTtl));
            if (Boolean.TRUE.equals(acquired)) {
                return () -> releaseLock(lockKey, owner);
            }
            if (System.nanoTime() >= deadlineNanos) {
                throw new OidcGatewayException("SESSION_LOCK_TIMEOUT", 503,
                        "Não foi possível adquirir o lock de refresh da sessão.");
            }
            try {
                Thread.sleep(LOCK_RETRY_DELAY_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new OidcGatewayException("SESSION_LOCK_TIMEOUT", 503,
                        "Lock de refresh interrompido.");
            }
        }
    }

    /**
     * A purga de expirados/tombstones é coberta pelo TTL nativo do Redis
     * (janela de tombstone na escrita) — mantida idempotente por contrato.
     */
    @Override
    public void purgeExpired() {
        // no-op: o TTL nativo do Redis é o mecanismo de purga.
    }

    @Override
    public int size() {
        Set<String> keys = redisOps(() -> redis.keys(SESSION_KEY_PREFIX + "*"));
        return keys == null ? 0 : keys.size();
    }

    private void releaseLock(String lockKey, String owner) {
        try {
            redisOps(() -> redis.execute(RELEASE_LOCK_SCRIPT, List.of(lockKey), owner));
        } catch (OidcGatewayException e) {
            log.warn("Failed to release gateway session lock; TTL will expire: key={}", lockKey);
        }
    }

    /**
     * Falhas de conexão/comando do Redis (RedisConnectionFailureException,
     * QueryTimeoutException etc. — todos {@link DataAccessException}) são
     * traduzidas para o erro de domínio {@code REDIS_UNAVAILABLE} (503). O
     * auth-service também acessa o banco relacional via JPA; traduzir aqui, no
     * adapter Redis, mantém a mensagem verdadeira sem contaminar outros
     * DataAccessException do web layer.
     */
    private <T> T redisOps(RedisOperation<T> operation) {
        try {
            return operation.execute();
        } catch (DataAccessException e) {
            log.warn("Redis unavailable during session operation: {}", e.getClass().getSimpleName());
            throw new OidcGatewayException("REDIS_UNAVAILABLE", 503,
                    "Dependência de sessão indisponível (Redis).");
        }
    }

    private void redisRun(RedisAction action) {
        try {
            action.execute();
        } catch (DataAccessException e) {
            log.warn("Redis unavailable during session operation: {}", e.getClass().getSimpleName());
            throw new OidcGatewayException("REDIS_UNAVAILABLE", 503,
                    "Dependência de sessão indisponível (Redis).");
        }
    }

    @FunctionalInterface
    private interface RedisOperation<T> {
        T execute();
    }

    @FunctionalInterface
    private interface RedisAction {
        void execute();
    }

    /**
     * TTL nativo = expiração efetiva + janela de tombstone. A janela mantém a
     * chave visível por alguns minutos após a expiração lógica para que a leitura
     * resolva {@code EXPIRED} (não {@code NOT_FOUND}); após a janela o Redis
     * remove a chave definitivamente.
     */
    private Duration redisTtl(GatewaySession session) {
        Instant now = Instant.now();
        if (session.isRevoked()) {
            long remaining = TOMBSTONE_RETENTION.toMillis()
                    - Duration.between(session.revokedAt(), now).toMillis();
            return Duration.ofMillis(Math.max(remaining, 1_000));
        }
        Duration ttl = Duration.between(now, session.effectiveExpiration(idleTimeout()));
        if (ttl.isNegative() || ttl.isZero()) {
            ttl = Duration.ZERO;
        }
        return ttl.plus(TOMBSTONE_RETENTION);
    }

    private Optional<GatewaySession> readValue(String key) {
        String json = redisOps(() -> redis.opsForValue().get(key));
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, GatewaySession.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize gateway session, removing key={}", key);
            redisOps(() -> redis.delete(key));
            return Optional.empty();
        }
    }

    private String serialize(GatewaySession session) {
        try {
            return objectMapper.writeValueAsString(session);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar sessão de gateway", e);
        }
    }

    private Duration idleTimeout() {
        return properties.getSessionIdleTimeout();
    }

    private static String sessionKey(String sessionToken) {
        return SESSION_KEY_PREFIX + sessionToken;
    }

    private static String lockKey(String sessionToken) {
        return LOCK_KEY_PREFIX + sessionToken;
    }
}
