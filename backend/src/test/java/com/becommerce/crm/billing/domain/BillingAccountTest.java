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
}
