package com.becommerce.auth.infrastructure.gateway;

import com.becommerce.auth.domain.gateway.PendingLink;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link PendingLinkStore} em memória (Sprint 7.2). Ativada por padrão
 * ({@code auth.gateway.session-store=memory}) e paralela ao
 * {@link InMemoryGatewaySessionStore}: nó único, suficiente para o fluxo de
 * vínculo de curta duração. A implementação distribuída é
 * {@link RedisPendingLinkStore}.
 */
@Component
@ConditionalOnProperty(name = "auth.gateway.session-store", havingValue = "memory", matchIfMissing = true)
public class InMemoryPendingLinkStore implements PendingLinkStore {

    private final Map<String, PendingLink> entries = new ConcurrentHashMap<>();

    @Override
    public void put(PendingLink pendingLink) {
        entries.put(pendingLink.token(), pendingLink);
    }

    @Override
    public Optional<PendingLink> get(String token) {
        if (token == null) {
            return Optional.empty();
        }
        PendingLink pendingLink = entries.get(token);
        if (pendingLink == null) {
            return Optional.empty();
        }
        if (pendingLink.isExpired(Instant.now())) {
            entries.remove(token);
            return Optional.empty();
        }
        return Optional.of(pendingLink);
    }

    @Override
    public void remove(String token) {
        if (token != null) {
            entries.remove(token);
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void purgeExpired() {
        Instant now = Instant.now();
        entries.entrySet().removeIf(e -> e.getValue().isExpired(now));
    }
}
