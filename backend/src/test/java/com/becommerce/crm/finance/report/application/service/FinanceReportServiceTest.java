package com.becommerce.crm.finance.report.application.service;

import com.becommerce.crm.finance.report.application.dto.AgendaReportResponse;
import com.becommerce.crm.finance.report.application.dto.CashFlowResponse;
import com.becommerce.crm.finance.report.application.dto.HeadlineResponse;
import com.becommerce.crm.finance.report.application.dto.HealthResponse;
import com.becommerce.crm.finance.report.application.dto.OverviewResponse;
import com.becommerce.crm.finance.report.application.dto.RevenueDistributionResponse;
import com.becommerce.crm.finance.report.application.dto.SummaryFilter;
import com.becommerce.crm.finance.report.application.dto.SummaryResponse;
import com.becommerce.crm.finance.report.application.port.output.FinanceReportGateway;
import com.becommerce.crm.finance.report.domain.ReportGranularity;
import com.becommerce.crm.finance.transaction.domain.TransactionStatus;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import com.becommerce.crm.finance.transaction.domain.exception.FinancialTransactionValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinanceReportServiceTest {

    private static final UUID COMPANY = UUID.randomUUID();

    private FinanceReportGateway gateway;
    private FinanceReportService service;

    @BeforeEach
    void setUp() {
        gateway = mock(FinanceReportGateway.class);
        service = new FinanceReportService(gateway);
    }

    // =========================================================================
    // Summary
    // =========================================================================

    @Test
    void summarySplitsPaidPendingAndIgnoresCancelled() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);
        when(gateway.summaryTotals(any(), any())).thenReturn(List.of(
                new FinanceReportGateway.SummarySumRow(TransactionType.INCOME,
                        TransactionStatus.PAID, 10000L),
                new FinanceReportGateway.SummarySumRow(TransactionType.EXPENSE,
                        TransactionStatus.PAID, 4000L),
                new FinanceReportGateway.SummarySumRow(TransactionType.INCOME,
                        TransactionStatus.PENDING, 5000L),
                new FinanceReportGateway.SummarySumRow(TransactionType.EXPENSE,
                        TransactionStatus.PENDING, 2000L),
                new FinanceReportGateway.SummarySumRow(TransactionType.INCOME,
                        TransactionStatus.CANCELLED, 999999L)));

        SummaryResponse response = service.summary(COMPANY, null, null, from, to,
                null, null, null, null, null);

        assertEquals(10000L, response.incomeTotalCents());
        assertEquals(4000L, response.expenseTotalCents());
        assertEquals(5000L, response.incomeForecastCents());
        assertEquals(2000L, response.expenseForecastCents());
        assertEquals(6000L, response.balanceCents());
    }

    @Test
    void summaryDefaultsRangeToCurrentMonth() {
        service.summary(COMPANY, null, null, null, null, null, null, null, null, null);

        ArgumentCaptor<SummaryFilter> captor = ArgumentCaptor.forClass(SummaryFilter.class);
        verify(gateway).summaryTotals(any(), captor.capture());
        LocalDate today = LocalDate.now();
        assertEquals(today.withDayOfMonth(1), captor.getValue().from());
        assertEquals(today.withDayOfMonth(today.lengthOfMonth()), captor.getValue().to());
    }

    @Test
    void summaryMapsOverdueToPendingWithDueDateLimit() {
        service.summary(COMPANY, null, "overdue", LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31), null, null, null, null, null);

        ArgumentCaptor<SummaryFilter> captor = ArgumentCaptor.forClass(SummaryFilter.class);
        verify(gateway).summaryTotals(any(), captor.capture());
        assertEquals(TransactionStatus.PENDING.name(), captor.getValue().status());
        assertEquals(LocalDate.now(), captor.getValue().overdueBefore());
    }

    @Test
    void summaryRejectsInvalidStatus() {
        assertThrows(FinancialTransactionValidationException.class,
                () -> service.summary(COMPANY, null, "PAGO", null, null, null, null, null, null,
                        null));
    }

    @Test
    void rejectsFromAfterTo() {
        assertThrows(FinancialTransactionValidationException.class,
                () -> service.summary(COMPANY, null, null, LocalDate.of(2026, 2, 1),
                        LocalDate.of(2026, 1, 1), null, null, null, null, null));
    }

    @Test
    void rejectsRangeLongerThanTwoYears() {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusYears(2).minusDays(1);
        assertThrows(FinancialTransactionValidationException.class,
                () -> service.cashFlow(COMPANY, ReportGranularity.MONTH, from, to));
    }

    // =========================================================================
    // Health
    // =========================================================================

    @Test
    void healthFillsEveryMonthWithZerosAndComputesBalance() {
        YearMonth first = YearMonth.from(LocalDate.now()).minusMonths(5);
        String key = first.toString();
        when(gateway.healthTotals(any(), any(), any())).thenReturn(List.of(
                new FinanceReportGateway.HealthMonthRow(key, TransactionType.INCOME, 6000L),
                new FinanceReportGateway.HealthMonthRow(key, TransactionType.EXPENSE, 2500L)));

        HealthResponse response = service.health(COMPANY, 6);

        assertEquals(6, response.months().size());
        assertEquals(key, response.months().get(0).month());
        assertEquals(6000L, response.months().get(0).incomeCents());
        assertEquals(2500L, response.months().get(0).expenseCents());
        assertEquals(3500L, response.months().get(0).balanceCents());
        assertEquals(YearMonth.from(LocalDate.now()).toString(), response.months().get(5).month());
        assertEquals(0L, response.months().get(5).incomeCents());
        assertEquals(0L, response.months().get(5).expenseCents());
        assertEquals(0L, response.months().get(5).balanceCents());
    }

    @Test
    void healthRejectsMonthsOutside6Or12() {
        assertThrows(FinancialTransactionValidationException.class, () -> service.health(COMPANY, 7));
        assertThrows(FinancialTransactionValidationException.class, () -> service.health(COMPANY, 3));
    }

    // =========================================================================
    // Cash flow
    // =========================================================================

    @Test
    void cashFlowBuildsSeriesWithOpenAndAccumulatedBalance() {
        LocalDate today = LocalDate.now();
        YearMonth current = YearMonth.from(today);
        LocalDate period = current.atDay(1);
        LocalDate next = current.plusMonths(1).atDay(1);
        LocalDate lastDayOfNext = current.plusMonths(1).atEndOfMonth();
        when(gateway.historyTotals(any(), any(), any(), any(), any())).thenReturn(List.of(
                new FinanceReportGateway.HistoryRow(period, TransactionType.INCOME, "PAID", 10000L),
                new FinanceReportGateway.HistoryRow(period, TransactionType.INCOME, "OPEN", 2000L),
                new FinanceReportGateway.HistoryRow(period, TransactionType.INCOME,
                        "FORECAST", 12000L),
                new FinanceReportGateway.HistoryRow(period, TransactionType.EXPENSE, "PAID", 3000L),
                new FinanceReportGateway.HistoryRow(period, TransactionType.EXPENSE,
                        "DEFAULT", 500L),
                new FinanceReportGateway.HistoryRow(next, TransactionType.INCOME, "PAID", 20000L),
                new FinanceReportGateway.HistoryRow(next, TransactionType.INCOME, "OPEN", 1000L)));
        when(gateway.paidIncomeByMethod(any(), any(), any())).thenReturn(List.of());

        CashFlowResponse response = service.cashFlow(COMPANY, ReportGranularity.MONTH,
                current.atDay(1), lastDayOfNext);

        assertEquals(2, response.series().size());
        CashFlowResponse.CashFlowSeriesItem first = response.series().get(0);
        assertEquals(10000L, first.incomeCents());
        assertEquals(2000L, first.openReceivablesCents());
        assertEquals(3000L, first.expenseCents());
        assertEquals(0L, first.openPayablesCents());
        assertEquals(7000L, first.periodBalanceCents());
        assertEquals(7000L, first.accumulatedBalanceCents());
        assertEquals(1000L, response.series().get(1).openReceivablesCents());
        assertEquals(27000L, response.series().get(1).accumulatedBalanceCents());

        assertEquals(12000L, response.incomeHistory().get(0).forecastCents());
        assertEquals(10000L, response.incomeHistory().get(0).incomeCents());
        assertEquals(0L, response.incomeHistory().get(0).defaultCents());
        assertEquals(2000L, response.incomeHistory().get(0).openCents());
        assertEquals(20000L, response.incomeHistory().get(1).incomeCents());
        assertEquals(0L, response.incomeHistory().get(1).forecastCents());

        assertEquals(3000L, response.expenseHistory().get(0).expenseCents());
        assertEquals(500L, response.expenseHistory().get(0).defaultCents());
        assertEquals(0L, response.expenseHistory().get(0).openCents());

        assertEquals(10000L, response.currentMonth().income().paidCents());
        assertEquals(2000L, response.currentMonth().income().openCents());
        assertEquals(0L, response.currentMonth().income().defaultCents());
        assertEquals(3000L, response.currentMonth().expense().paidCents());
        assertEquals(500L, response.currentMonth().expense().defaultCents());
        assertEquals(0L, response.currentMonth().expense().openCents());
    }

    @Test
    void cashFlowIncomeByMethodDefaultsMissingMethodsToZero() {
        LocalDate today = LocalDate.now();
        LocalDate from = today.withDayOfMonth(1);
        LocalDate to = today.withDayOfMonth(today.lengthOfMonth());
        when(gateway.historyTotals(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(gateway.paidIncomeByMethod(any(), any(), any())).thenReturn(
                List.of(new FinanceReportGateway.MethodTotalsRow("PIX", 5000L)));

        CashFlowResponse response = service.cashFlow(COMPANY, ReportGranularity.MONTH, from, to);

        assertEquals(5000L, response.incomeByMethod().pixCents());
        assertEquals(0L, response.incomeByMethod().boletoCents());
        assertEquals(0L, response.incomeByMethod().creditCardCents());
        assertEquals(0L, response.incomeByMethod().debitCardCents());
        assertEquals(0L, response.incomeByMethod().cashCents());
        assertEquals(0L, response.incomeByMethod().transferCents());
        assertEquals(0L, response.incomeByMethod().checkCents());
        assertEquals(0L, response.incomeByMethod().otherCents());
    }

    // =========================================================================
    // Revenue distribution
    // =========================================================================

    @Test
    void revenueDistributionIncludesAverageNetAndProfessionalFallback() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);
        when(gateway.revenueByProcedure(any(), any(), any(), any(), anyList())).thenReturn(List.of(
                new FinanceReportGateway.ProcedureTotalsRow(from, UUID.randomUUID(),
                        "Limpeza", 15000L)));
        when(gateway.revenueAverageByProcedure(any(), any(), any(), anyList())).thenReturn(List.of(
                new FinanceReportGateway.ProcedureAverageRow(UUID.randomUUID(), "Limpeza",
                        15000L, 12000L)));
        when(gateway.revenueByProfessional(any(), any(), any(), any())).thenReturn(List.of(
                new FinanceReportGateway.ProfessionalTotalsRow(from, null, "Sem profissional",
                        8000L)));

        RevenueDistributionResponse response = service.revenueDistribution(COMPANY,
                ReportGranularity.MONTH, from, to, null);

        assertEquals("2026-01", response.byProcedure().get(0).period());
        assertEquals("Limpeza", response.byProcedure().get(0).name());
        assertEquals(15000L, response.byProcedure().get(0).amountCents());
        assertEquals(15000L, response.averageByProcedure().get(0).averageCents());
        assertEquals(12000L, response.averageByProcedure().get(0).averageNetCents());
        assertEquals("Sem profissional", response.byProfessional().get(0).name());
        assertEquals(8000L, response.byProfessional().get(0).amountCents());
    }

    // =========================================================================
    // Overview
    // =========================================================================

    @Test
    void overviewComputesRatesTotalsAndSeries() {
        LocalDate today = LocalDate.now();
        LocalDate period = today.withDayOfMonth(1);
        LocalDate from = today.withDayOfMonth(1);
        LocalDate to = today.withDayOfMonth(today.lengthOfMonth());
        UUID professional = UUID.randomUUID();
        when(gateway.practitioners(any(), any(), any(), any())).thenReturn(
                List.of(new FinanceReportGateway.UserRow(professional, "Dr. Ana")));
        when(gateway.appointmentStatusByPeriod(any(), any(), any(), any(), anyList()))
                .thenReturn(List.of(
                        new FinanceReportGateway.StatusPeriodCountRow(period, "SCHEDULED", 3L),
                        new FinanceReportGateway.StatusPeriodCountRow(period, "CONFIRMED", 5L),
                        new FinanceReportGateway.StatusPeriodCountRow(period, "COMPLETED", 8L),
                        new FinanceReportGateway.StatusPeriodCountRow(period, "NO_SHOW", 2L)));
        when(gateway.procedureCountsByPeriod(any(), any(), any(), any())).thenReturn(List.of(
                new FinanceReportGateway.ProcedureCountRow(period, "DONE", 10L),
                new FinanceReportGateway.ProcedureCountRow(period, "PENDING", 2L)));
        when(gateway.historyTotals(any(), any(), any(), any(), any())).thenReturn(List.of(
                new FinanceReportGateway.HistoryRow(period, TransactionType.INCOME, "PAID",
                        50000L)));

        OverviewResponse response = service.overview(COMPANY, ReportGranularity.MONTH,
                from, to, null);

        assertEquals(1, response.professionals().size());
        assertEquals("Dr. Ana", response.professionals().get(0).name());
        assertEquals(18L, response.totals().appointments());
        assertEquals(27.8, response.totals().confirmationRate(), 1e-9);
        assertEquals(80.0, response.totals().attendanceRate(), 1e-9);
        assertEquals(10L, response.totals().proceduresDone());
        assertEquals(50000L, response.totals().revenueCents());
        assertEquals(8L, response.appointmentsByAttendance().get(0).attended());
        assertEquals(2L, response.appointmentsByAttendance().get(0).notAttended());
        assertEquals(8L, response.appointmentsByAttendance().get(0).pending());
        assertEquals(27.8, response.confirmed().get(0).rate(), 1e-9);
        assertEquals(80.0, response.attended().get(0).rate(), 1e-9);
        assertEquals(10L, response.procedures().get(0).done());
        assertEquals(2L, response.procedures().get(0).pending());
        assertEquals(50000L, response.revenue().get(0).amountCents());
    }

    @Test
    void overviewAttendanceRateIsZeroWhenNoConcludedAppointments() {
        LocalDate today = LocalDate.now();
        LocalDate period = today.withDayOfMonth(1);
        LocalDate from = today.withDayOfMonth(1);
        LocalDate to = today.withDayOfMonth(today.lengthOfMonth());
        when(gateway.practitioners(any(), any(), any(), any())).thenReturn(List.of());
        when(gateway.appointmentStatusByPeriod(any(), any(), any(), any(), anyList()))
                .thenReturn(List.of(
                        new FinanceReportGateway.StatusPeriodCountRow(period, "SCHEDULED", 4L)));
        when(gateway.procedureCountsByPeriod(any(), any(), any(), any())).thenReturn(List.of());
        when(gateway.historyTotals(any(), any(), any(), any(), any())).thenReturn(List.of());

        OverviewResponse response = service.overview(COMPANY, ReportGranularity.MONTH,
                from, to, null);

        assertEquals(4L, response.totals().appointments());
        assertEquals(0.0, response.totals().attendanceRate(), 1e-9);
        assertEquals(0.0, response.totals().confirmationRate(), 1e-9);
    }

    // =========================================================================
    // Agenda report
    // =========================================================================

    @Test
    void agendaReportMapsExternalStatusesAndComputesTotals() {
        when(gateway.agendaByStatus(any(), any(), any(), isNull(), anyList(), anyList()))
                .thenReturn(List.of(
                        new FinanceReportGateway.AgendaStatusRow("SCHEDULED", 4L),
                        new FinanceReportGateway.AgendaStatusRow("CONFIRMED", 3L),
                        new FinanceReportGateway.AgendaStatusRow("CANCELED", 2L),
                        new FinanceReportGateway.AgendaStatusRow("COMPLETED", 6L),
                        new FinanceReportGateway.AgendaStatusRow("NO_SHOW", 1L)));

        AgendaReportResponse response = service.agendaReport(COMPANY,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), null, null, null);

        assertEquals(16L, response.total());
        assertEquals(6L, response.attended());
        assertEquals(1L, response.notAttended());
        assertEquals(85.7, response.attendanceRate(), 1e-9);
        assertEquals(5, response.byStatus().size());
        assertEquals(List.of("SCHEDULED", "CONFIRMED", "CANCELED", "COMPLETED", "NO_SHOW"),
                response.byStatus().stream().map(AgendaReportResponse.StatusCountItem::status)
                        .toList());
    }

    @Test
    void agendaReportMapsConfirmedPendingCancelado() {
        when(gateway.agendaByStatus(any(), any(), any(), isNull(), anyList(), anyList()))
                .thenReturn(List.of());

        service.agendaReport(COMPANY, LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31), null, List.of("CANCELADO", "CONFIRMADO", "PENDENTE"),
                null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(gateway).agendaByStatus(any(), any(), any(), isNull(), captor.capture(), anyList());
        assertEquals(List.of("CANCELED", "CONFIRMED", "SCHEDULED"), captor.getValue());
    }

    @Test
    void agendaReportRejectsUnknownExternalStatus() {
        assertThrows(FinancialTransactionValidationException.class,
                () -> service.agendaReport(COMPANY, LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 31), null, List.of("NAO_SEI"), null));
    }

    // =========================================================================
    // Headline
    // =========================================================================

    @Test
    void headlineCollectsMonthRevenueWeekAttendancesAndBudgets() {
        when(gateway.paidIncome(any(), any(), any())).thenReturn(42000L);
        when(gateway.attendancesThisWeek(any(), any(), any())).thenReturn(9L);
        when(gateway.budgetsCreatedThisMonth(any(), any(), any())).thenReturn(3L);

        HeadlineResponse response = service.headline(COMPANY);

        assertEquals(42000L, response.revenueThisMonthCents());
        assertEquals(9L, response.attendancesThisWeek());
        assertEquals(3L, response.budgetsCreatedThisMonth());
    }
}