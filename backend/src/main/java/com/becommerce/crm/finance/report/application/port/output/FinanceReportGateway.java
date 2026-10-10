package com.becommerce.crm.finance.report.application.port.output;

import com.becommerce.crm.finance.report.application.dto.SummaryFilter;
import com.becommerce.crm.finance.report.domain.ReportGranularity;
import com.becommerce.crm.finance.transaction.domain.TransactionStatus;
import com.becommerce.crm.finance.transaction.domain.TransactionType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Porta de saída dos relatórios financeiros (Etapa 2, somente leitura). Todas as
 * consultas são SELECT nativos sobre {@code financial_transactions} (V092) e leitura
 * direta de {@code appointments}, {@code appointment_types}, {@code opportunities},
 * {@code users} e {@code catalog_items} — o isolamento por tenant é o RLS FORCE
 * (V019 registra as policies; V034 concede SELECT ao crm_app).
 */
public interface FinanceReportGateway {

    /** Somas por tipo e status (PAID/PENDING) do período; CANCELLED passa pelo filtro. */
    List<SummarySumRow> summaryTotals(UUID companyId, SummaryFilter filter);

    /** Somas por período, tipo e bucket (PAID/OPEN/DEFAULT/FORECAST) — base do cash-flow. */
    List<HistoryRow> historyTotals(UUID companyId, ReportGranularity granularity, LocalDate from,
                                   LocalDate to, LocalDate today);

    /** Receitas INCOME PAID do período por forma de pagamento. */
    List<MethodTotalsRow> paidIncomeByMethod(UUID companyId, LocalDate from, LocalDate to);

    /** Receitas INCOME PAID somadas do período. */
    long paidIncome(UUID companyId, LocalDate from, LocalDate to);

    /** Somas mensais (yyyy-MM) de receitas/despesas PAID. */
    List<HealthMonthRow> healthTotals(UUID companyId, LocalDate from, LocalDate to);

    /** Receitas PAID por procedimento (catalog_item_id) e período. */
    List<ProcedureTotalsRow> revenueByProcedure(UUID companyId, ReportGranularity granularity,
                                                LocalDate from, LocalDate to,
                                                List<UUID> catalogItemIds);

    /** Médias (amount e amount - fee) das receitas PAID por procedimento. */
    List<ProcedureAverageRow> revenueAverageByProcedure(UUID companyId, LocalDate from, LocalDate to,
                                                        List<UUID> catalogItemIds);

    /** Receitas PAID por profissional e período; profissional vazio = "Sem profissional". */
    List<ProfessionalTotalsRow> revenueByProfessional(UUID companyId, ReportGranularity granularity,
                                                      LocalDate from, LocalDate to);

    /** Profissionais do período; com {@code preferredIds} vazio, usa os hostes com agenda. */
    List<UserRow> practitioners(UUID companyId, LocalDate from, LocalDate to,
                                List<UUID> preferredIds);

    /** Contagens de agendamento por status e período (profissional opcional). */
    List<StatusPeriodCountRow> appointmentStatusByPeriod(UUID companyId, ReportGranularity granularity,
                                                         LocalDate from, LocalDate to,
                                                         List<UUID> professionalUserIds);

    /** Contagens por status da agenda no período (statuses já mapeados pela service). */
    List<AgendaStatusRow> agendaByStatus(UUID companyId, LocalDate from, LocalDate to,
                                         UUID appointmentType, List<String> statuses,
                                         List<UUID> professionalUserIds);

    /** Contagens de lançamentos INCOME com procedimento (DONE = PAID, PENDING) por período. */
    List<ProcedureCountRow> procedureCountsByPeriod(UUID companyId, ReportGranularity granularity,
                                                    LocalDate from, LocalDate to);

    /** Agendamentos COMPLETED entre {@code weekStart} e {@code weekEnd}. */
    long attendancesThisWeek(UUID companyId, LocalDate weekStart, LocalDate weekEnd);

    /** Oportunidades criadas entre {@code monthStart} e {@code nextMonthStart}. */
    long budgetsCreatedThisMonth(UUID companyId, LocalDate monthStart, LocalDate nextMonthStart);

    record SummarySumRow(TransactionType type, TransactionStatus status, long amountCents) {}

    record HistoryRow(LocalDate period, TransactionType type, String bucket, long amountCents) {}

    record MethodTotalsRow(String paymentMethod, long amountCents) {}

    record HealthMonthRow(String month, TransactionType type, long amountCents) {}

    record ProcedureTotalsRow(LocalDate period, UUID catalogItemId, String name, long amountCents) {}

    record ProcedureAverageRow(UUID catalogItemId, String name, long averageCents,
                               long averageNetCents) {}

    record ProfessionalTotalsRow(LocalDate period, UUID professionalUserId, String name,
                                 long amountCents) {}

    record UserRow(UUID id, String name) {}

    record StatusPeriodCountRow(LocalDate period, String status, long count) {}

    record AgendaStatusRow(String status, long count) {}

    record ProcedureCountRow(LocalDate period, String bucket, long count) {}
}