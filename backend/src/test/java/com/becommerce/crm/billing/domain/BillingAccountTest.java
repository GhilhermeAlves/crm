package com.becommerce.crm.billing.domain;

import com.becommerce.crm.masterdata.company.domain.CompanyPlan;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class BillingAccountTest {
    private final UUID company = UUID.randomUUID();

    @Test
    void startsNotStartedWithPlanPrice() {
        BillingAccount a = BillingAccount.start(company, CompanyPlan.STARTER, 19700);
        assertEquals(BillingStatus.NOT_STARTED, a.getStatus());
        assertEquals(19700, a.getMonthlyPriceCents());
        assertEquals(CompanyPlan.STARTER, a.getPlan());
        assertTrue(a.isChargeable());
    }

    @Test
    void activatedStoresAsaasIdsAndClearsError() {
        BillingAccount a = BillingAccount.start(company, CompanyPlan.STARTER, 19700);
        a.syncFailed("timeout");
        a.activated("cus_1", "sub_1", LocalDate.of(2026, 11, 15));
        assertEquals(BillingStatus.ACTIVE, a.getStatus());
        assertEquals("cus_1", a.getAsaasCustomerId());
        assertEquals("sub_1", a.getAsaasSubscriptionId());
        assertEquals(LocalDate.of(2026, 11, 15), a.getNextDueDate());
        assertNull(a.getSyncError());
    }

    @Test
    void syncFailedKeepsIdsAndTruncatesError() {
        BillingAccount a = BillingAccount.start(company, CompanyPlan.STARTER, 19700);
        a.syncFailed("x".repeat(600));
        assertEquals(BillingStatus.SYNC_PENDING, a.getStatus());
        assertEquals(500, a.getSyncError().length());
    }

    @Test
    void exemptIsNotChargeable() {
        BillingAccount a = BillingAccount.start(company, CompanyPlan.STARTER, 19700);
        a.markExempt();
        assertEquals(BillingStatus.EXEMPT, a.getStatus());
        assertFalse(a.isChargeable());
    }

    @Test
    void changePriceRejectsNonPositive() {
        BillingAccount a = BillingAccount.start(company, CompanyPlan.STARTER, 19700);
        assertThrows(IllegalArgumentException.class, () -> a.changePrice(CompanyPlan.STARTER, 0));
        a.changePrice(CompanyPlan.BUSINESS, 79700);
        assertEquals(CompanyPlan.BUSINESS, a.getPlan());
        assertEquals(79700, a.getMonthlyPriceCents());
    }

    private BillingAccount active() {
        BillingAccount a = BillingAccount.start(company, CompanyPlan.STARTER, 19700);
        a.activated("cus_1", "sub_1", LocalDate.of(2026, 11, 15));
        return a;
    }

    @Test
    void overdueMovesActiveToPastDue() {
        BillingAccount a = active();
        a.onOverdue(LocalDate.of(2026, 11, 15));
        assertEquals(BillingStatus.PAST_DUE, a.getStatus());
        assertEquals(LocalDate.of(2026, 11, 15), a.getOverdueSince());
    }

    @Test
    void overdueIgnoredWhenExempt() {
        BillingAccount a = active();
        a.markExempt();
        a.onOverdue(LocalDate.of(2026, 11, 15));
        assertEquals(BillingStatus.EXEMPT, a.getStatus());
        assertNull(a.getOverdueSince());
    }

    @Test
    void blocksOnlyAfterTenDays() {
        BillingAccount a = active();
        a.onOverdue(LocalDate.of(2026, 11, 15));
        assertFalse(a.shouldBlock(LocalDate.of(2026, 11, 24)));
        assertTrue(a.shouldBlock(LocalDate.of(2026, 11, 25)));
    }

    @Test
    void graceDelaysBlockUntilItExpires() {
        BillingAccount a = active();
        a.onOverdue(LocalDate.of(2026, 11, 15));
        a.grant(LocalDate.of(2026, 11, 30));
        assertFalse(a.shouldBlock(LocalDate.of(2026, 11, 30)));
        assertTrue(a.shouldBlock(LocalDate.of(2026, 12, 1)));
    }

    @Test
    void grantUnblocksAndPaymentReactivates() {
        BillingAccount a = active();
        a.onOverdue(LocalDate.of(2026, 11, 15));
        a.block();
        assertEquals(BillingStatus.BLOCKED, a.getStatus());
        a.grant(LocalDate.of(2026, 11, 30));
        assertEquals(BillingStatus.PAST_DUE, a.getStatus());
        a.block();
        a.onPaid();
        assertEquals(BillingStatus.ACTIVE, a.getStatus());
        assertNull(a.getOverdueSince());
        assertNull(a.getGraceUntil());
    }

    @Test
    void blockOnlyFromPastDue() {
        BillingAccount a = active();
        a.block();
        assertEquals(BillingStatus.ACTIVE, a.getStatus());
    }

    @Test
    void expireGraceClearsPastDate() {
        BillingAccount a = active();
        a.grant(LocalDate.of(2026, 11, 20));
        a.expireGrace(LocalDate.of(2026, 11, 20));
        assertEquals(LocalDate.of(2026, 11, 20), a.getGraceUntil());
        a.expireGrace(LocalDate.of(2026, 11, 21));
        assertNull(a.getGraceUntil());
    }

    @Test
    void nextDueOnlyMovesForward() {
        BillingAccount a = active();
        a.nextDueAt(LocalDate.of(2026, 11, 10));
        assertEquals(LocalDate.of(2026, 11, 15), a.getNextDueDate());
        a.nextDueAt(LocalDate.of(2026, 12, 15));
        assertEquals(LocalDate.of(2026, 12, 15), a.getNextDueDate());
    }

    @Test
    void priceSyncPendingOnlyWithSubscriptionAndError() {
        BillingAccount a = active();
        assertFalse(a.isPriceSyncPending());
        a.priceSyncFailed("timeout");
        assertTrue(a.isPriceSyncPending());
    }
}
