package com.becommerce.crm.clinical.trichoscopy.live;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiveViewRegistryTest {

    private static final class MutableClock extends Clock {
        Instant now = Instant.parse("2026-10-30T10:00:00Z");

        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private final MutableClock clock = new MutableClock();
    private final LiveViewRegistry registry = new LiveViewRegistry(clock);
    private final UUID companyId = UUID.randomUUID();

    @Test
    void viewerReceivesLatestFrameWithIncreasingSeq() {
        var tokens = registry.open(companyId);
        assertNotEquals(tokens.publishToken(), tokens.viewToken());
        assertTrue(registry.latest(tokens.viewToken()).orElseThrow().isEmpty());

        registry.push(tokens.publishToken(), new byte[]{1});
        registry.push(tokens.publishToken(), new byte[]{2});

        var frame = registry.latest(tokens.viewToken()).orElseThrow().orElseThrow();
        assertArrayEquals(new byte[]{2}, frame.jpeg());
        assertEquals(2, frame.seq());
    }

    @Test
    void viewTokenCannotPublish() {
        var tokens = registry.open(companyId);
        assertFalse(registry.push(tokens.viewToken(), new byte[]{1}));
    }

    @Test
    void sessionExpiresWhenIdleAndOnClose() {
        var tokens = registry.open(companyId);
        clock.now = clock.now.plus(LiveViewRegistry.IDLE_TIMEOUT).plus(Duration.ofSeconds(1));
        assertTrue(registry.latest(tokens.viewToken()).isEmpty());

        var other = registry.open(companyId);
        registry.close(other.publishToken());
        assertTrue(registry.latest(other.viewToken()).isEmpty());
        assertFalse(registry.push(other.publishToken(), new byte[]{1}));
    }

    @Test
    void rejectsOversizedFrame() {
        var tokens = registry.open(companyId);
        assertThrows(IllegalArgumentException.class,
                () -> registry.push(tokens.publishToken(), new byte[LiveViewRegistry.MAX_FRAME_BYTES + 1]));
    }
}
