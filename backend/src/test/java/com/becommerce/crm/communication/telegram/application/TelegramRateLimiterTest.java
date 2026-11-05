package com.becommerce.crm.communication.telegram.application;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelegramRateLimiterTest {

    @Test
    void allowsTwentyPerMinuteThenBlocks() {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-04T10:00:00Z"));
        TelegramRateLimiter l = new TelegramRateLimiter(clock, 20);
        UUID co = UUID.randomUUID();
        for (int i = 0; i < 20; i++) {
            assertTrue(l.tryAcquire(co, 1L));
        }
        assertFalse(l.tryAcquire(co, 1L));
        assertTrue(l.tryAcquire(co, 2L));
        clock.advance(Duration.ofSeconds(61));
        assertTrue(l.tryAcquire(co, 1L));
    }

    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
