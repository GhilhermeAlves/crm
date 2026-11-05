package com.becommerce.crm.communication.telegram.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Limita mensagens por remetente do Telegram (janela deslizante de 60s). */
@Component
public class TelegramRateLimiter {

    private final Clock clock;
    private final int perMinute;
    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();

    @Autowired
    public TelegramRateLimiter(Clock clock) {
        this(clock, 20);
    }

    TelegramRateLimiter(Clock clock, int perMinute) {
        this.clock = clock;
        this.perMinute = perMinute;
    }

    public boolean tryAcquire(UUID companyId, long telegramUserId) {
        Instant now = clock.instant();
        Instant cutoff = now.minusSeconds(60);
        Deque<Instant> q = hits.computeIfAbsent(companyId + ":" + telegramUserId, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && q.peekFirst().isBefore(cutoff)) {
                q.pollFirst();
            }
            if (q.size() >= perMinute) {
                return false;
            }
            q.addLast(now);
            return true;
        }
    }
}
