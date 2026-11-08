package com.becommerce.crm.billing.application;

import com.becommerce.crm.billing.domain.BillingAccount;
import com.becommerce.crm.billing.infrastructure.BillingAccountJpaRepository;
import com.becommerce.crm.masterdata.company.domain.CompanyPlan;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BillingAccessServiceTest {
    private final UUID companyId = UUID.randomUUID();
    private final BillingAccountJpaRepository accounts = mock(BillingAccountJpaRepository.class);
    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-26T12:00:00Z"));
    private final BillingAccessService service = new BillingAccessService(accounts, clock);

    private BillingAccount blocked() {
        BillingAccount a = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        a.activated("cus_1", "sub_1", LocalDate.of(2026, 11, 15));
        a.onOverdue(LocalDate.of(2026, 11, 15));
        a.block();
        return a;
    }

    @Test
    void blockedAccountIsBlockedAndCached() {
        when(accounts.findById(companyId)).thenReturn(Optional.of(blocked()));
        assertTrue(service.isBlocked(companyId));
        assertTrue(service.isBlocked(companyId));
        verify(accounts, times(1)).findById(companyId);
    }

    @Test
    void cacheExpiresAfter60sAndEvictForcesReload() {
        when(accounts.findById(companyId)).thenReturn(Optional.empty());
        assertFalse(service.isBlocked(companyId));
        clock.plusSeconds(61);
        service.isBlocked(companyId);
        service.evict(companyId);
        service.isBlocked(companyId);
        verify(accounts, times(3)).findById(companyId);
    }

    static final class MutableClock extends Clock {
        private Instant now;
        MutableClock(Instant now) { this.now = now; }
        void plusSeconds(long s) { now = now.plusSeconds(s); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
