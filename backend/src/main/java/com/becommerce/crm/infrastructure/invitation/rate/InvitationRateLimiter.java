package com.becommerce.crm.infrastructure.invitation.rate;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter simples em memória (janela deslizante) para convites (Sprint 8.5).
 * Sem dependência externa. Em cenário multi-instância deve ser substituído por
 * uma implementação distribuída (Redis/DB), mas atende ao deploy único atual.
 *
 * <p>Limita a criação de convites por empresa e as tentativas de aceite por
 * usuário, reduzindo abuso/força bruta de token.</p>
 */
@Component
public class InvitationRateLimiter {

    private static final int MAX_CREATES_PER_WINDOW = 20;
    private static final int MAX_ACCEPTS_PER_WINDOW = 10;
    private static final Duration WINDOW = Duration.ofMinutes(60);

    private final ConcurrentHashMap<String, long[]> counters = new ConcurrentHashMap<>();

    public boolean tryCreate(String companyId) {
        return permit("create:" + companyId, MAX_CREATES_PER_WINDOW);
    }

    public boolean tryAccept(String key) {
        return permit("accept:" + key, MAX_ACCEPTS_PER_WINDOW);
    }

    private boolean permit(String key, int max) {
        long now = System.currentTimeMillis();
        long windowStart = now - WINDOW.toMillis();
        counters.compute(key, (k, current) -> {
            // Reinicia a janela quando ela já expirou; senão incrementa o contador.
            if (current == null || current[0] < windowStart) {
                return new long[]{now, 1};
            }
            current[1]++;
            return current;
        });
        long[] entry = counters.get(key);
        return entry == null || entry[1] <= max;
    }

    // Cleanup oportunista p/ não acumular chaves de janelas antigas indefinidamente.
    public void prune() {
        long now = System.currentTimeMillis();
        long windowStart = now - WINDOW.toMillis();
        for (Map.Entry<String, long[]> e : counters.entrySet()) {
            if (e.getValue()[0] < windowStart) {
                counters.remove(e.getKey());
            }
        }
    }
}