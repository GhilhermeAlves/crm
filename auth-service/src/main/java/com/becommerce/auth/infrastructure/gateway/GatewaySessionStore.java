package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.GatewaySession;
import com.becommerce.auth.domain.gateway.SessionLookup;

import java.util.Optional;

/**
 * Abstração de storage das sessões de browser do Access Gateway (Sprints 6.1/6.2).
 *
 * <p>A sessão é criada somente após CRM Access positivo e referenciada pelo
 * {@code sessionToken} opaco do cookie HttpOnly. O ciclo de vida (6.2) é
 * preservado por qualquer implementação:
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
 * <p>Implementações:
 * <ul>
 *   <li>{@link InMemoryGatewaySessionStore} — memória local (nó único,
 *       profile/teste);</li>
 *   <li>{@link RedisGatewaySessionStore} — Redis compartilhado entre réplicas,
 *       com TTL nativo e lock distribuído por sessão (Sprint 6.3).</li>
 * </ul>
 */
public interface GatewaySessionStore {

    void put(GatewaySession session);

    Optional<GatewaySession> get(String sessionToken);

    SessionLookup findByToken(String sessionToken);

    /**
     * Revoga a sessão deixando um tombstone (idempotente). Um replay do cookie
     * antigo passa a resolver {@code REVOKED}.
     */
    void revoke(String sessionToken);

    void remove(String sessionToken);

    /**
     * Lock por sessão para serializar a rotação de tokens no refresh. Distribuído
     * (Redis) quando há múltiplas réplicas — {@code synchronized} local não serve.
     */
    GatewaySessionLock lockFor(String sessionToken);

    /**
     * Purga sessões expiradas e tombstones além da retenção. No Redis a purga é
     * coberta pelo TTL nativo; a chamada permanece idempotente.
     */
    void purgeExpired();

    int size();
}
