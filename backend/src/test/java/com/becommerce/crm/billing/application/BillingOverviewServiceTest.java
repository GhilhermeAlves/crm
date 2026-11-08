package com.becommerce.crm.billing.application;

import com.becommerce.crm.billing.domain.BillingAccount;
import com.becommerce.crm.billing.infrastructure.BillingAccountJpaRepository;
import com.becommerce.crm.masterdata.company.domain.CompanyPlan;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BillingOverviewServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final BillingAccountJpaRepository accounts = mock(BillingAccountJpaRepository.class);
    private final BillingOverviewService service = new BillingOverviewService(jdbc, accounts);

    @Test
    void oneStatusPerCompany() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        when(jdbc.queryForList(anyString(), eq(UUID.class))).thenReturn(List.of(a, b, c));
        BillingAccount acc = BillingAccount.start(a, CompanyPlan.STARTER, 19700);
        acc.activated("cus", "sub", LocalDate.of(2026, 11, 15));
        when(accounts.findById(a)).thenReturn(Optional.of(acc));
        when(accounts.findById(b)).thenReturn(Optional.empty());
        when(accounts.findById(c)).thenThrow(new RuntimeException("db"));

        var result = service.statuses();

        assertEquals(List.of(
                new BillingOverviewService.CompanyBillingStatus(a, "ACTIVE"),
                new BillingOverviewService.CompanyBillingStatus(b, "NOT_STARTED"),
                new BillingOverviewService.CompanyBillingStatus(c, "UNKNOWN")), result);
    }
}
