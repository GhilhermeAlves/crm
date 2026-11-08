package com.becommerce.crm.billing.application;

import com.becommerce.crm.billing.domain.BillingAccount;
import com.becommerce.crm.billing.domain.BillingStatus;
import com.becommerce.crm.billing.infrastructure.BillingAccountJpaRepository;
import com.becommerce.crm.masterdata.company.domain.CompanyPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BillingDailyJobTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-26T09:30:00Z"), ZoneOffset.UTC);
    private final UUID companyId = UUID.randomUUID();
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final BillingAccountJpaRepository accounts = mock(BillingAccountJpaRepository.class);
    private final BillingService billing = mock(BillingService.class);
    private final BillingPaymentService payments = mock(BillingPaymentService.class);
    private final BillingAccessService access = mock(BillingAccessService.class);
    private BillingAccount acc;

    private BillingDailyJob job(boolean enabled) {
        return new BillingDailyJob(jdbc, accounts, billing, payments, access,
                new BillingProperties(enabled, "u", enabled ? "KEY" : "", "W"), CLOCK);
    }

    @BeforeEach
    void setUp() {
        when(jdbc.queryForList(anyString(), eq(UUID.class))).thenReturn(List.of(companyId));
        acc = BillingAccount.start(companyId, CompanyPlan.STARTER, 19700);
        acc.activated("cus_1", "sub_1", LocalDate.of(2026, 11, 15));
        when(accounts.findById(companyId)).thenReturn(Optional.of(acc));
        when(accounts.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void blocksAfterTenDaysAndReconciles() {
        acc.onOverdue(LocalDate.of(2026, 11, 16));
        job(true).run();
        assertEquals(BillingStatus.BLOCKED, acc.getStatus());
        verify(billing).retrySync(companyId);
        verify(payments).refreshOpen(companyId, LocalDate.of(2026, 11, 26));
        verify(access).evict(companyId);
    }

    @Test
    void doesNotBlockBeforeTenDays() {
        acc.onOverdue(LocalDate.of(2026, 11, 17));
        job(true).run();
        assertEquals(BillingStatus.PAST_DUE, acc.getStatus());
    }

    @Test
    void respectsGrace() {
        acc.onOverdue(LocalDate.of(2026, 11, 1));
        acc.grant(LocalDate.of(2026, 11, 26));
        job(true).run();
        assertEquals(BillingStatus.PAST_DUE, acc.getStatus());
    }

    @Test
    void expiredGraceIsClearedAndBlocks() {
        acc.onOverdue(LocalDate.of(2026, 11, 1));
        acc.grant(LocalDate.of(2026, 11, 25));
        job(true).run();
        assertNull(acc.getGraceUntil());
        assertEquals(BillingStatus.BLOCKED, acc.getStatus());
    }

    @Test
    void disabledStillBlocksButDoesNotCallAsaas() {
        acc.onOverdue(LocalDate.of(2026, 11, 1));
        job(false).run();
        assertEquals(BillingStatus.BLOCKED, acc.getStatus());
        verifyNoInteractions(billing, payments);
    }

    @Test
    void exemptIsSkipped() {
        acc.markExempt();
        job(true).run();
        verifyNoInteractions(billing, payments);
        verify(accounts, never()).saveAndFlush(any());
    }

    @Test
    void failureInOneCompanyDoesNotStopTheJob() {
        UUID other = UUID.randomUUID();
        when(jdbc.queryForList(anyString(), eq(UUID.class))).thenReturn(List.of(other, companyId));
        when(accounts.findById(other)).thenThrow(new RuntimeException("db"));
        acc.onOverdue(LocalDate.of(2026, 11, 1));
        job(true).run();
        assertEquals(BillingStatus.BLOCKED, acc.getStatus());
    }
}
