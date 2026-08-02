package com.becommerce.crm.infrastructure.tenant.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldSetAndGetCompanyId() {
        UUID companyId = UUID.randomUUID();
        TenantContext.setCompanyId(companyId);
        assertEquals(companyId, TenantContext.getCompanyId());
    }

    @Test
    void shouldReturnNullWhenNotSet() {
        assertNull(TenantContext.getCompanyId());
    }

    @Test
    void shouldClearCompanyId() {
        TenantContext.setCompanyId(UUID.randomUUID());
        TenantContext.clear();
        assertNull(TenantContext.getCompanyId());
    }

    @Test
    void shouldReportHasCompanyId() {
        assertFalse(TenantContext.hasCompanyId());
        TenantContext.setCompanyId(UUID.randomUUID());
        assertTrue(TenantContext.hasCompanyId());
        TenantContext.clear();
        assertFalse(TenantContext.hasCompanyId());
    }

    @Test
    void shouldNotLeakBetweenThreads() throws InterruptedException {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        AtomicReference<UUID> threadAResult = new AtomicReference<>();
        AtomicReference<UUID> threadBResult = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(2);

        TenantContext.setCompanyId(tenantA);

        Thread threadA = new Thread(() -> {
            TenantContext.setCompanyId(tenantA);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            threadAResult.set(TenantContext.getCompanyId());
            TenantContext.clear();
            latch.countDown();
        });

        Thread threadB = new Thread(() -> {
            TenantContext.setCompanyId(tenantB);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            threadBResult.set(TenantContext.getCompanyId());
            TenantContext.clear();
            latch.countDown();
        });

        threadA.start();
        threadB.start();
        latch.await();

        assertEquals(tenantA, threadAResult.get());
        assertEquals(tenantB, threadBResult.get());
        assertEquals(tenantA, TenantContext.getCompanyId());
    }

    @Test
    void shouldClearInFinallyBlock() {
        UUID companyId = UUID.randomUUID();
        TenantContext.setCompanyId(companyId);

        try {
            assertTrue(TenantContext.hasCompanyId());
            throw new RuntimeException("test");
        } catch (RuntimeException e) {
            // expected
        } finally {
            TenantContext.clear();
        }

        assertFalse(TenantContext.hasCompanyId());
    }
}
