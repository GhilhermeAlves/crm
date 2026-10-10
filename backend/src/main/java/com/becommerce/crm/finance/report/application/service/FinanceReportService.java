package com.becommerce.crm.finance.report.application.service;

import com.becommerce.crm.finance.report.application.dto.AgendaReportResponse;
import com.becommerce.crm.finance.report.application.dto.CashFlowResponse;
import com.becommerce.crm.finance.report.application.dto.CashFlowResponse.CashFlowHistoryItem;
import com.becommerce.crm.finance.report.application.dto.CashFlowResponse.CashFlowSeriesItem;
import com.becommerce.crm.finance.report.application.dto.CashFlowResponse.CurrentMonth;
import com.becommerce.crm.finance.report.application.dto.CashFlowResponse.ExpenseHistoryItem;
import com.becommerce.crm.finance.report.application.dto.CashFlowResponse.IncomeByMethod;
import com.becommerce.crm.finance.report.application.dto.CashFlowResponse.MonthAmounts;
import com.becommerce.crm.finance.report.application.dto.HeadlineResponse;
import com.becommerce.crm.finance.report.application.dto.HealthResponse;
import com.becommerce.crm.finance.report.application.dto.HealthResponse.MonthHealth;
import com.becommerce.crm.finance.report.application.dto.OverviewResponse;
import com.becommerce.crm.finance.report.application.dto.OverviewResponse.AttendanceItem;
import com.becommerce.crm.finance.report.application.dto.OverviewResponse.OverviewTotals;
import com.becommerce.crm.finance.report.application.dto.OverviewResponse.ProceduresItem;
import com.becommerce.crm.finance.report.application.dto.OverviewResponse.ProfessionalItem;
import com.becommerce.crm.finance.report.application.dto.OverviewResponse.RateItem;
import com.becommerce.crm.finance.report.application.dto.OverviewResponse.RevenueItem;
import com.becommerce.crm.finance.report.application.dto.RevenueDistributionResponse;
import com.becommerce.crm.finance.report.application.dto.RevenueDistributionResponse.RevenueAverageItem;
import com.becommerce.crm.finance.report.application.dto.RevenueDistributionResponse.RevenueByProcedureItem;
import com.becommerce.crm.finance.report.application.dto.RevenueDistributionResponse.RevenueByProfessionalItem;
import com.becommerce.crm.finance.report.application.dto.SummaryFilter;
import com.becommerce.crm.finance.report.application.dto.SummaryResponse;
import com.becommerce.crm.finance.report.application.port.output.FinanceReportGateway;
import com.becommerce.crm.finance.report.domain.ReportGranularity;
import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionStatus;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import com.becommerce.crm.finance.transaction.domain.exception.FinancialTransactionValidationException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Relatórios financeiros (Etapa 2, somente leitura). Valores em centavos;
 * CANCELLED nunca entra em soma; "inadimplente/default" = PENDING com vencimento
 * anterior a hoje. Agrega sobre linhas brutas da {@link FinanceReportGateway} e
 * completa os períodos sem movimento com zero.
 */
@Service
public class FinanceReportService {

    private static final String BUCKET_PAID = "PAID";
    private static final String BUCKET_OPEN = "OPEN";
    private static final String BUCKET_DEFAULT = "DEFAULT";
    private static final String BUCKET_FORECAST = "FORECAST";
    private static final String BUCKET_DONE = "DONE";
    private static final String BUCKET_PENDING = "PENDING";
    private static final String OVERDUE = "OVERDUE";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_NO_SHOW = "NO_SHOW";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_SCHEDULED = "SCHEDULED";
    private static final int HEALTH_MONTHS_DEFAULT = 6;
    private static final int HEALTH_MONTHS_MAX = 12;

    private final FinanceReportGateway gateway;

    public FinanceReportService(FinanceReportGateway gateway) {
        this.gateway = gateway;
    }

    // =========================================================================
    // /summary
    // =========================================================================

    @Transactional(readOnly = true)
    public SummaryResponse summary(UUID companyId, TransactionType type, String status,
                                   LocalDate from, LocalDate to, PaymentMethod paymentMethod,
                                   TransactionCategory category, UUID professionalUserId,
                                   UUID contactId, String query) {
        LocalDate[] range = resolveRange(from, to);
        SummaryFilter filter = buildFilter(type, status, range[0], range[1], paymentMethod,
                category, professionalUserId, contactId, pattern(query));
        try {
            TenantContext.setCompanyId(companyId);
            long incomePaid = 0L;
            long expensePaid = 0L;
            long incomePending = 0L;
            long expensePending = 0L;
            for (FinanceReportGateway.SummarySumRow row : gateway.summaryTotals(companyId, filter)) {
                if (row.status() == TransactionStatus.PAID) {
                    if (row.type() == TransactionType.INCOME) {
                        incomePaid += row.amountCents();
                    } else {
                        expensePaid += row.amountCents();
                    }
                } else if (row.status() == TransactionStatus.PENDING) {
                    if (row.type() == TransactionType.INCOME) {
                        incomePending += row.amountCents();
                    } else {
                        expensePending += row.amountCents();
                    }
                }
            }
            return new SummaryResponse(incomePaid, expensePaid, incomePending, expensePending,
                    incomePaid - expensePaid);
        } finally {
            TenantContext.clear();
        }
    }

    // =========================================================================
    // /health
    // =========================================================================

    @Transactional(readOnly = true)
    public HealthResponse health(UUID companyId, int months) {
        if (months != HEALTH_MONTHS_DEFAULT && months != HEALTH_MONTHS_MAX) {
            throw new FinancialTransactionValidationException(
                    "months deve ser " + HEALTH_MONTHS_DEFAULT + " ou " + HEALTH_MONTHS_MAX + ".");
        }
        LocalDate today = LocalDate.now();
        YearMonth firstMonth = YearMonth.from(today).minusMonths(months - 1L);
        LocalDate from = firstMonth.atDay(1);
        LocalDate to = today.withDayOfMonth(today.lengthOfMonth());
        try {
            TenantContext.setCompanyId(companyId);
            Map<String, long[]> pairByMonth = new HashMap<>();
            for (FinanceReportGateway.HealthMonthRow row : gateway.healthTotals(companyId, from, to)) {
                long[] pair = pairByMonth.computeIfAbsent(row.month(), key -> new long[2]);
                if (row.type() == TransactionType.INCOME) {
                    pair[0] += row.amountCents();
                } else {
                    pair[1] += row.amountCents();
                }
            }
            List<MonthHealth> monthsList = new ArrayList<>();
            YearMonth month = firstMonth;
            YearMonth currentMonth = YearMonth.from(today);
            while (!month.isAfter(currentMonth)) {
                String key = month.toString();
                long[] pair = pairByMonth.getOrDefault(key, new long[2]);
                monthsList.add(new MonthHealth(key, pair[0], pair[1], pair[0] - pair[1]));
                month = month.plusMonths(1);
            }
            return new HealthResponse(monthsList);
        } finally {
            TenantContext.clear();
        }
    }

    // =========================================================================
    // /cash-flow
    // =========================================================================

    @Transactional(readOnly = true)
    public CashFlowResponse cashFlow(UUID companyId, ReportGranularity granularity,
                                     LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        LocalDate currentStart = today.withDayOfMonth(1);
        LocalDate currentEnd = today.withDayOfMonth(today.lengthOfMonth());
        LocalDate[] user = resolveRange(from, to);
        // Estende a janela para cobrir o mês corrente (usada no currentMonth) e
        // o primeiro período truncado, para não perder parcelas da virada do período.
        LocalDate effectiveFrom = min(user[0], granularity.truncate(currentStart));
        LocalDate effectiveTo = max(user[1], currentEnd);
        try {
            TenantContext.setCompanyId(companyId);
            List<LocalDate> userPeriods = periods(granularity, user[0], user[1]);
            Map<LocalDate, PeriodAggregates> aggregates = new LinkedHashMap<>();
            for (FinanceReportGateway.HistoryRow row : gateway.historyTotals(companyId, granularity,
                    effectiveFrom, effectiveTo, today)) {
                aggregates.computeIfAbsent(row.period(), key -> new PeriodAggregates()).apply(row);
            }
            long accumulated = 0L;
            List<CashFlowSeriesItem> series = new ArrayList<>();
            List<CashFlowHistoryItem> incomeHistory = new ArrayList<>();
            List<ExpenseHistoryItem> expenseHistory = new ArrayList<>();
            for (LocalDate periodStart : userPeriods) {
                PeriodAggregates agg = aggregates.getOrDefault(periodStart, PeriodAggregates.EMPTY);
                long periodBalance = agg.incomePaid - agg.expensePaid;
                accumulated += periodBalance;
                String label = granularity.label(periodStart);
                series.add(new CashFlowSeriesItem(label, agg.incomePaid, agg.openReceivables,
                        agg.expensePaid, agg.openPayables, periodBalance, accumulated));
                incomeHistory.add(new CashFlowHistoryItem(label, agg.forecastIncome, agg.incomePaid,
                        agg.defaultIncome, agg.openIncome));
                expenseHistory.add(new ExpenseHistoryItem(label, agg.expensePaid, agg.openExpense,
                        agg.defaultExpense));
            }
            CurrentMonthTotals current = new CurrentMonthTotals();
            for (LocalDate periodStart : periods(granularity, currentStart, currentEnd)) {
                PeriodAggregates agg = aggregates.getOrDefault(periodStart, PeriodAggregates.EMPTY);
                current.incomePaid += agg.incomePaid;
                current.incomeOpen += agg.openIncome;
                current.incomeDefault += agg.defaultIncome;
                current.expensePaid += agg.expensePaid;
                current.expenseOpen += agg.openExpense;
                current.expenseDefault += agg.defaultExpense;
            }
            Map<PaymentMethod, Long> byMethod = new EnumMap<>(PaymentMethod.class);
            for (FinanceReportGateway.MethodTotalsRow row : gateway.paidIncomeByMethod(companyId,
                    user[0], user[1])) {
                byMethod.merge(PaymentMethod.valueOf(row.paymentMethod()), row.amountCents(),
                        Long::sum);
            }
            CashFlowResponse response = new CashFlowResponse(series,
                    new IncomeByMethod(amount(PaymentMethod.BOLETO, byMethod),
                            amount(PaymentMethod.CREDIT_CARD, byMethod),
                            amount(PaymentMethod.DEBIT_CARD, byMethod),
                            amount(PaymentMethod.PIX, byMethod),
                            amount(PaymentMethod.CASH, byMethod),
                            amount(PaymentMethod.TRANSFER, byMethod),
                            amount(PaymentMethod.CHECK, byMethod),
                            amount(PaymentMethod.OTHER, byMethod)),
                    incomeHistory, expenseHistory,
                    new CurrentMonth(
                            new MonthAmounts(current.incomePaid, current.incomeOpen,
                                    current.incomeDefault),
                            new MonthAmounts(current.expensePaid, current.expenseOpen,
                                    current.expenseDefault)));
            return response;
        } finally {
            TenantContext.clear();
        }
    }

    // =========================================================================
    // /revenue-distribution
    // =========================================================================

    @Transactional(readOnly = true)
    public RevenueDistributionResponse revenueDistribution(UUID companyId,
                                                           ReportGranularity granularity,
                                                           LocalDate from, LocalDate to,
                                                           List<UUID> catalogItemIds) {
        LocalDate[] range = resolveRange(from, to);
        List<UUID> catalogItems = clean(catalogItemIds);
        try {
            TenantContext.setCompanyId(companyId);
            List<RevenueByProcedureItem> byProcedure = new ArrayList<>();
            for (FinanceReportGateway.ProcedureTotalsRow row : gateway.revenueByProcedure(companyId,
                    granularity, range[0], range[1], catalogItems)) {
                byProcedure.add(new RevenueByProcedureItem(granularity.label(row.period()),
                        row.catalogItemId(), row.name(), row.amountCents()));
            }
            List<RevenueAverageItem> averageByProcedure = new ArrayList<>();
            for (FinanceReportGateway.ProcedureAverageRow row : gateway.revenueAverageByProcedure(
                    companyId, range[0], range[1], catalogItems)) {
                averageByProcedure.add(new RevenueAverageItem(row.catalogItemId(), row.name(),
                        row.averageCents(), row.averageNetCents()));
            }
            List<RevenueByProfessionalItem> byProfessional = new ArrayList<>();
            for (FinanceReportGateway.ProfessionalTotalsRow row : gateway.revenueByProfessional(
                    companyId, granularity, range[0], range[1])) {
                byProfessional.add(new RevenueByProfessionalItem(granularity.label(row.period()),
                        row.professionalUserId(), row.name(), row.amountCents()));
            }
            return new RevenueDistributionResponse(byProcedure, averageByProcedure, byProfessional);
        } finally {
            TenantContext.clear();
        }
    }

    // =========================================================================
    // /overview
    // =========================================================================

    @Transactional(readOnly = true)
    public OverviewResponse overview(UUID companyId, ReportGranularity granularity, LocalDate from,
                                     LocalDate to, List<UUID> professionalUserIds) {
        LocalDate[] range = resolveRange(from, to);
        List<UUID> professionals = clean(professionalUserIds);
        try {
            TenantContext.setCompanyId(companyId);
            List<ProfessionalItem> professionalItems = new ArrayList<>();
            for (FinanceReportGateway.UserRow user : gateway.practitioners(companyId, range[0],
                    range[1], professionals)) {
                professionalItems.add(new ProfessionalItem(user.id(), user.name()));
            }
            List<LocalDate> periodStarts = periods(granularity, range[0], range[1]);
            Map<LocalDate, StatusCounts> counts = new LinkedHashMap<>();
            for (LocalDate periodStart : periodStarts) {
                counts.put(periodStart, new StatusCounts());
            }
            for (FinanceReportGateway.StatusPeriodCountRow row : gateway.appointmentStatusByPeriod(
                    companyId, granularity, range[0], range[1], professionals)) {
                counts.getOrDefault(row.period(), new StatusCounts()).add(row.status(), row.count());
            }
            List<AttendanceItem> attendance = new ArrayList<>();
            List<RateItem> confirmedSeries = new ArrayList<>();
            List<RateItem> attendedSeries = new ArrayList<>();
            long totalAppointments = 0L;
            long totalConfirmed = 0L;
            long totalAttended = 0L;
            long totalNotAttended = 0L;
            for (LocalDate periodStart : periodStarts) {
                StatusCounts sc = counts.get(periodStart);
                long attended = sc.count(STATUS_COMPLETED);
                long notAttended = sc.count(STATUS_NO_SHOW);
                long pending = sc.count(STATUS_SCHEDULED) + sc.count(STATUS_CONFIRMED);
                long confirmed = sc.count(STATUS_CONFIRMED);
                long periodTotal = attended + notAttended + pending;
                totalAppointments += periodTotal;
                totalConfirmed += confirmed;
                totalAttended += attended;
                totalNotAttended += notAttended;
                String label = granularity.label(periodStart);
                attendance.add(new AttendanceItem(label, attended, notAttended, pending));
                confirmedSeries.add(new RateItem(label, rate(confirmed, periodTotal)));
                attendedSeries.add(new RateItem(label, rate(attended, attended + notAttended)));
            }
            Map<LocalDate, ProcedureCounts> proceduresByPeriod = new LinkedHashMap<>();
            for (LocalDate periodStart : periodStarts) {
                proceduresByPeriod.put(periodStart, new ProcedureCounts());
            }
            for (FinanceReportGateway.ProcedureCountRow row : gateway.procedureCountsByPeriod(
                    companyId, granularity, range[0], range[1])) {
                ProcedureCounts pc = proceduresByPeriod.get(row.period());
                if (pc != null) {
                    if (BUCKET_DONE.equals(row.bucket())) {
                        pc.done += row.count();
                    } else if (BUCKET_PENDING.equals(row.bucket())) {
                        pc.pending += row.count();
                    }
                }
            }
            List<ProceduresItem> proceduresSeries = new ArrayList<>();
            long proceduresDone = 0L;
            for (LocalDate periodStart : periodStarts) {
                ProcedureCounts pc = proceduresByPeriod.get(periodStart);
                proceduresDone += pc.done;
                proceduresSeries.add(new ProceduresItem(granularity.label(periodStart), pc.done,
                        pc.pending));
            }
            Map<LocalDate, Long> revenueByPeriod = new HashMap<>();
            for (FinanceReportGateway.HistoryRow row : gateway.historyTotals(companyId, granularity,
                    range[0], range[1], LocalDate.now())) {
                if (BUCKET_PAID.equals(row.bucket())
                        && row.type() == TransactionType.INCOME) {
                    revenueByPeriod.merge(row.period(), row.amountCents(), Long::sum);
                }
            }
            List<RevenueItem> revenueSeries = new ArrayList<>();
            long revenueCents = 0L;
            for (LocalDate periodStart : periodStarts) {
                long amount = revenueByPeriod.getOrDefault(periodStart, 0L);
                revenueCents += amount;
                revenueSeries.add(new RevenueItem(granularity.label(periodStart), amount));
            }
            return new OverviewResponse(professionalItems,
                    new OverviewTotals(totalAppointments, rate(totalConfirmed, totalAppointments),
                            rate(totalAttended, totalAttended + totalNotAttended), proceduresDone,
                            revenueCents),
                    attendance, confirmedSeries, attendedSeries, proceduresSeries, revenueSeries);
        } finally {
            TenantContext.clear();
        }
    }

    // =========================================================================
    // /agenda-report
    // =========================================================================

    @Transactional(readOnly = true)
    public AgendaReportResponse agendaReport(UUID companyId, LocalDate from, LocalDate to,
                                             UUID appointmentType, List<String> statuses,
                                             List<UUID> professionalUserIds) {
        List<String> mapped = mapAgendaStatuses(statuses);
        List<UUID> professionals = clean(professionalUserIds);
        LocalDate[] range = resolveRange(from, to);
        try {
            TenantContext.setCompanyId(companyId);
            long total = 0L;
            long attended = 0L;
            long notAttended = 0L;
            List<AgendaReportResponse.StatusCountItem> byStatus = new ArrayList<>();
            for (FinanceReportGateway.AgendaStatusRow row : gateway.agendaByStatus(companyId,
                    range[0], range[1], appointmentType, mapped, professionals)) {
                total += row.count();
                if (STATUS_COMPLETED.equals(row.status())) {
                    attended += row.count();
                } else if (STATUS_NO_SHOW.equals(row.status())) {
                    notAttended += row.count();
                }
                byStatus.add(new AgendaReportResponse.StatusCountItem(row.status(), row.count()));
            }
            return new AgendaReportResponse(total, attended, notAttended,
                    rate(attended, attended + notAttended), byStatus);
        } finally {
            TenantContext.clear();
        }
    }

    // =========================================================================
    // /headline
    // =========================================================================

    @Transactional(readOnly = true)
    public HeadlineResponse headline(UUID companyId) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
        LocalDate nextMonthStart = monthStart.plusMonths(1);
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        try {
            TenantContext.setCompanyId(companyId);
            return new HeadlineResponse(
                    gateway.paidIncome(companyId, monthStart, monthEnd),
                    gateway.attendancesThisWeek(companyId, weekStart, weekEnd),
                    gateway.budgetsCreatedThisMonth(companyId, monthStart, nextMonthStart));
        } finally {
            TenantContext.clear();
        }
    }

    // =========================================================================
    // Filtros e périodos
    // =========================================================================

    private SummaryFilter buildFilter(TransactionType type, String status, LocalDate from,
                                      LocalDate to, PaymentMethod paymentMethod,
                                      TransactionCategory category, UUID professionalUserId,
                                      UUID contactId, String pattern) {
        LocalDate overdueBefore = null;
        String normalizedStatus = null;
        if (isOverdue(status)) {
            normalizedStatus = TransactionStatus.PENDING.name();
            overdueBefore = LocalDate.now();
        } else if (status != null && !status.isBlank()) {
            normalizedStatus = parseStatus(status).name();
        }
        return new SummaryFilter(type, normalizedStatus, overdueBefore, from, to, paymentMethod,
                category, professionalUserId, contactId, pattern);
    }

    private LocalDate[] resolveRange(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        if (from == null) {
            from = today.withDayOfMonth(1);
        }
        if (to == null) {
            to = today.withDayOfMonth(today.lengthOfMonth());
        }
        if (from.isAfter(to)) {
            throw new FinancialTransactionValidationException(
                    "A data inicial não pode ser posterior à final.");
        }
        if (from.plusYears(2).isBefore(to)) {
            throw new FinancialTransactionValidationException(
                    "O período máximo de relatório é de 2 anos.");
        }
        return new LocalDate[]{from, to};
    }

    private static List<LocalDate> periods(ReportGranularity granularity, LocalDate from,
                                           LocalDate to) {
        List<LocalDate> starts = new ArrayList<>();
        LocalDate current = granularity.truncate(from);
        while (!current.isAfter(to)) {
            starts.add(current);
            current = granularity.next(current);
        }
        return starts;
    }

    private static List<String> mapAgendaStatuses(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        List<String> mapped = new ArrayList<>();
        for (String status : statuses) {
            if (status == null || status.isBlank()) {
                continue;
            }
            switch (status.trim().toUpperCase(Locale.ROOT)) {
                case "CANCELADO" -> mapped.add("CANCELED");
                case "CONFIRMADO" -> mapped.add(STATUS_CONFIRMED);
                case "PENDENTE" -> mapped.add(STATUS_SCHEDULED);
                default -> throw new FinancialTransactionValidationException(
                        "Status da agenda inválido: " + status + ".");
            }
        }
        return mapped;
    }

    private static boolean isOverdue(String status) {
        return status != null && OVERDUE.equalsIgnoreCase(status.trim());
    }

    private static TransactionStatus parseStatus(String status) {
        try {
            return TransactionStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new FinancialTransactionValidationException("Status inválido: " + status + ".");
        }
    }

    /** {@code pattern} já vem minúsculo com {@code %...%} (ou nulo = sem busca textual). */
    private static String pattern(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return "%" + escapeLike(query.trim().toLowerCase(Locale.ROOT)) + "%";
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static List<UUID> clean(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    private static long amount(PaymentMethod method, Map<PaymentMethod, Long> byMethod) {
        return byMethod.getOrDefault(method, 0L);
    }

    /** Taxa percentual 0-100 com 1 casa decimal; denominador zero retorna 0. */
    private static double rate(long part, long total) {
        if (total == 0L) {
            return 0.0;
        }
        return Math.round(part * 1000.0 / total) / 10.0;
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? b : a;
    }

    // =========================================================================
    // Acumuladores internos
    // =========================================================================

    private static final class PeriodAggregates {

        static final PeriodAggregates EMPTY = new PeriodAggregates();

        long incomePaid;
        long openReceivables;
        long expensePaid;
        long openPayables;
        long forecastIncome;
        long defaultIncome;
        long openIncome;
        long defaultExpense;
        long openExpense;

        void apply(FinanceReportGateway.HistoryRow row) {
            long value = row.amountCents();
            boolean income = row.type() == TransactionType.INCOME;
            switch (row.bucket()) {
                case BUCKET_PAID -> {
                    if (income) {
                        incomePaid += value;
                    } else {
                        expensePaid += value;
                    }
                }
                case BUCKET_OPEN -> {
                    if (income) {
                        openReceivables += value;
                        openIncome += value;
                    } else {
                        openPayables += value;
                        openExpense += value;
                    }
                }
                case BUCKET_DEFAULT -> {
                    if (income) {
                        defaultIncome += value;
                    } else {
                        defaultExpense += value;
                    }
                }
                case BUCKET_FORECAST -> {
                    if (income) {
                        forecastIncome += value;
                    }
                }
                default -> {
                }
            }
        }
    }

    /** Somatório do mês corrente por tipo (paid/open/default). */
    private static final class CurrentMonthTotals {
        long incomePaid;
        long incomeOpen;
        long incomeDefault;
        long expensePaid;
        long expenseOpen;
        long expenseDefault;
    }

    private static final class StatusCounts {
        private final Map<String, Long> counts = new HashMap<>();

        void add(String status, long count) {
            counts.merge(status, count, Long::sum);
        }

        long count(String status) {
            return counts.getOrDefault(status, 0L);
        }
    }

    private static final class ProcedureCounts {
        long done;
        long pending;
    }
}