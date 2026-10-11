package com.becommerce.crm.finance.report.infrastructure.persistence;

import com.becommerce.crm.finance.report.application.dto.SummaryFilter;
import com.becommerce.crm.finance.report.application.port.output.FinanceReportGateway;
import com.becommerce.crm.finance.report.domain.ReportGranularity;
import com.becommerce.crm.finance.transaction.domain.TransactionStatus;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Consultas SQL nativas de leitura dos relatórios (Etapa 2). O {@code JdbcTemplate}
 * usa o {@code DataSource} embrulhado pelo {@code TenantDataSourcePostProcessor}, então
 * as GUCs de tenant (app.current_company_id) são aplicadas por statement e o RLS FORCE
 * isola os dados — mantendo também {@code company_id} explícito nos filtros.
 *
 * <p>Datas da agenda ({@code start_at} TIMESTAMPTZ) são interpretadas em UTC.
 */
@Repository
public class FinanceReportGatewayImpl implements FinanceReportGateway {

    private static final String UNASSIGNED_PROFESSIONAL = "Sem profissional";

    private final NamedParameterJdbcTemplate jdbc;

    public FinanceReportGatewayImpl(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<SummarySumRow> SUMMARY_ROW = (rs, i) ->
            new SummarySumRow(
                    TransactionType.valueOf(rs.getString("type")),
                    TransactionStatus.valueOf(rs.getString("status")),
                    rs.getLong("total"));

    private static final RowMapper<HistoryRow> HISTORY_ROW = (rs, i) ->
            new HistoryRow(
                    rs.getDate("period").toLocalDate(),
                    TransactionType.valueOf(rs.getString("type")),
                    rs.getString("bucket"),
                    rs.getLong("amount"));

    private static final RowMapper<MethodTotalsRow> METHOD_ROW = (rs, i) ->
            new MethodTotalsRow(rs.getString("payment_method"), rs.getLong("total"));

    private static final RowMapper<HealthMonthRow> HEALTH_ROW = (rs, i) ->
            new HealthMonthRow(
                    rs.getString("month"),
                    TransactionType.valueOf(rs.getString("type")),
                    rs.getLong("total"));

    private static final RowMapper<ProcedureTotalsRow> PROCEDURE_ROW = (rs, i) ->
            new ProcedureTotalsRow(
                    rs.getDate("period").toLocalDate(),
                    rs.getObject("catalog_item_id", UUID.class),
                    rs.getString("name"),
                    rs.getLong("total"));

    private static final RowMapper<ProcedureAverageRow> AVERAGE_ROW = (rs, i) ->
            new ProcedureAverageRow(
                    rs.getObject("catalog_item_id", UUID.class),
                    rs.getString("name"),
                    rs.getLong("average"),
                    rs.getLong("average_net"));

    private static final RowMapper<ProfessionalTotalsRow> PROFESSIONAL_ROW = (rs, i) ->
            new ProfessionalTotalsRow(
                    rs.getDate("period").toLocalDate(),
                    rs.getObject("professional_user_id", UUID.class),
                    rs.getString("name"),
                    rs.getLong("total"));

    private static final RowMapper<UserRow> USER_ROW = (rs, i) ->
            new UserRow(rs.getObject("id", UUID.class), rs.getString("name"));

    private static final RowMapper<StatusPeriodCountRow> STATUS_PERIOD_ROW = (rs, i) ->
            new StatusPeriodCountRow(
                    rs.getDate("period").toLocalDate(),
                    rs.getString("status"),
                    rs.getLong("total"));

    private static final RowMapper<AgendaStatusRow> AGENDA_ROW = (rs, i) ->
            new AgendaStatusRow(rs.getString("status"), rs.getLong("total"));

    private static final RowMapper<ProcedureCountRow> PROCEDURE_COUNT_ROW = (rs, i) ->
            new ProcedureCountRow(rs.getDate("period").toLocalDate(), rs.getString("bucket"),
                    rs.getLong("total"));

    @Override
    public List<SummarySumRow> summaryTotals(UUID companyId, SummaryFilter filter) {
        String sql = "SELECT ft.type AS type, ft.status AS status, SUM(ft.amount_cents) AS total "
                + "FROM financial_transactions ft "
                + "WHERE ft.company_id = :companyId "
                + "AND (CAST(:type AS VARCHAR) IS NULL OR ft.type = :type) "
                + "AND (CAST(:paymentMethod AS VARCHAR) IS NULL OR ft.payment_method = :paymentMethod) "
                + "AND (CAST(:category AS VARCHAR) IS NULL OR ft.category = :category) "
                + "AND (CAST(:professionalUserId AS UUID) IS NULL OR ft.professional_user_id = :professionalUserId) "
                + "AND (CAST(:contactId AS UUID) IS NULL OR ft.contact_id = :contactId) "
                + "AND (CAST(:pattern AS VARCHAR) IS NULL OR LOWER(ft.description) LIKE :pattern) "
                + "AND (CAST(:status AS VARCHAR) IS NULL OR ft.status = :status) "
                + "AND (CAST(:overdueBefore AS DATE) IS NULL OR ft.due_date < :overdueBefore) "
                + "AND ((ft.status = 'PAID' AND ft.paid_at::date BETWEEN :from AND :to) "
                + "OR (ft.status = 'PENDING' AND ft.due_date BETWEEN :from AND :to)) "
                + "GROUP BY ft.type, ft.status ORDER BY ft.type, ft.status";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("companyId", companyId)
                .addValue("type", filter.type() == null ? null : filter.type().name())
                .addValue("paymentMethod", filter.paymentMethod() == null ? null
                        : filter.paymentMethod().name())
                .addValue("category", filter.category() == null ? null : filter.category().name())
                .addValue("professionalUserId", filter.professionalUserId())
                .addValue("contactId", filter.contactId())
                .addValue("pattern", filter.pattern())
                .addValue("status", filter.status())
                .addValue("overdueBefore", filter.overdueBefore())
                .addValue("from", filter.from())
                .addValue("to", filter.to());
        return jdbc.query(sql, params, SUMMARY_ROW);
    }

    @Override
    public List<HistoryRow> historyTotals(UUID companyId, ReportGranularity granularity,
                                          LocalDate from, LocalDate to, LocalDate today) {
        String trunc = granularity.trunc();
        String sql = String.format("""
                        SELECT period, type, bucket, amount
                        FROM (
                            SELECT date_trunc('%s', ft.paid_at::timestamp)::date AS period,
                                   ft.type AS type, 'PAID' AS bucket,
                                   SUM(ft.amount_cents) AS amount
                            FROM financial_transactions ft
                            WHERE ft.company_id = :companyId AND ft.status = 'PAID'
                              AND ft.paid_at::date BETWEEN :from AND :to
                            GROUP BY 1, 2
                            UNION ALL
                            SELECT date_trunc('%s', ft.due_date::timestamp)::date, ft.type,
                                   'FORECAST', SUM(ft.amount_cents)
                            FROM financial_transactions ft
                            WHERE ft.company_id = :companyId
                              AND ft.status IN ('PAID', 'PENDING')
                              AND ft.due_date >= :from AND ft.due_date <= :to
                            GROUP BY 1, 2
                            UNION ALL
                            SELECT date_trunc('%s', ft.due_date::timestamp)::date, ft.type,
                                   'OPEN', SUM(ft.amount_cents)
                            FROM financial_transactions ft
                            WHERE ft.company_id = :companyId AND ft.status = 'PENDING'
                              AND ft.due_date >= :today
                              AND ft.due_date >= :from AND ft.due_date <= :to
                            GROUP BY 1, 2
                            UNION ALL
                            SELECT date_trunc('%s', ft.due_date::timestamp)::date, ft.type,
                                   'DEFAULT', SUM(ft.amount_cents)
                            FROM financial_transactions ft
                            WHERE ft.company_id = :companyId AND ft.status = 'PENDING'
                              AND ft.due_date < :today
                              AND ft.due_date >= :from AND ft.due_date <= :to
                            GROUP BY 1, 2
                        ) aggregation
                        ORDER BY period, bucket, type
                        """, trunc, trunc, trunc, trunc);
        MapSqlParameterSource params = baseParams(companyId, from, to);
        params.addValue("today", today);
        return jdbc.query(sql, params, HISTORY_ROW);
    }

    @Override
    public List<MethodTotalsRow> paidIncomeByMethod(UUID companyId, LocalDate from, LocalDate to) {
        String sql = "SELECT COALESCE(ft.payment_method, 'OTHER') AS payment_method, "
                + "SUM(ft.amount_cents) AS total "
                + "FROM financial_transactions ft "
                + "WHERE ft.company_id = :companyId AND ft.type = 'INCOME' AND ft.status = 'PAID' "
                + "AND ft.paid_at::date BETWEEN :from AND :to "
                + "GROUP BY 1 ORDER BY 1";
        return jdbc.query(sql, baseParams(companyId, from, to), METHOD_ROW);
    }

    @Override
    public long paidIncome(UUID companyId, LocalDate from, LocalDate to) {
        String sql = "SELECT COALESCE(SUM(ft.amount_cents), 0) AS total "
                + "FROM financial_transactions ft "
                + "WHERE ft.company_id = :companyId AND ft.type = 'INCOME' AND ft.status = 'PAID' "
                + "AND ft.paid_at::date BETWEEN :from AND :to";
        Long total = jdbc.queryForObject(sql, baseParams(companyId, from, to), Long.class);
        return total == null ? 0L : total;
    }

    @Override
    public List<HealthMonthRow> healthTotals(UUID companyId, LocalDate from, LocalDate to) {
        String sql = "SELECT to_char(ft.paid_at, 'YYYY-MM') AS month, ft.type AS type, "
                + "SUM(ft.amount_cents) AS total "
                + "FROM financial_transactions ft "
                + "WHERE ft.company_id = :companyId AND ft.status = 'PAID' "
                + "AND ft.paid_at::date BETWEEN :from AND :to "
                + "GROUP BY 1, 2 ORDER BY 1";
        return jdbc.query(sql, baseParams(companyId, from, to), HEALTH_ROW);
    }

    @Override
    public List<ProcedureTotalsRow> revenueByProcedure(UUID companyId,
                                                       ReportGranularity granularity,
                                                       LocalDate from, LocalDate to,
                                                       List<UUID> catalogItemIds) {
        boolean hasFilter = !catalogItemIds.isEmpty();
        String sql = "SELECT date_trunc('" + granularity.trunc()
                + "', ft.paid_at::timestamp)::date AS period, "
                + "ci.id AS catalog_item_id, ci.name AS name, SUM(ft.amount_cents) AS total "
                + "FROM financial_transactions ft "
                + "JOIN catalog_items ci ON ci.id = ft.catalog_item_id "
                + "WHERE ft.company_id = :companyId AND ft.type = 'INCOME' AND ft.status = 'PAID' "
                + "AND ft.paid_at::date BETWEEN :from AND :to "
                + (hasFilter ? "AND ft.catalog_item_id IN (:catalogItemIds) " : "")
                + "GROUP BY 1, 2, 3 ORDER BY 1, 2";
        MapSqlParameterSource params = baseParams(companyId, from, to);
        if (hasFilter) {
            params.addValue("catalogItemIds", catalogItemIds);
        }
        return jdbc.query(sql, params, PROCEDURE_ROW);
    }

    @Override
    public List<ProcedureAverageRow> revenueAverageByProcedure(UUID companyId, LocalDate from,
                                                               LocalDate to,
                                                               List<UUID> catalogItemIds) {
        boolean hasFilter = !catalogItemIds.isEmpty();
        String sql = "SELECT ci.id AS catalog_item_id, ci.name AS name, "
                + "ROUND(AVG(ft.amount_cents))::bigint AS average, "
                + "ROUND(AVG(ft.amount_cents - ft.fee_cents))::bigint AS average_net "
                + "FROM financial_transactions ft "
                + "JOIN catalog_items ci ON ci.id = ft.catalog_item_id "
                + "WHERE ft.company_id = :companyId AND ft.type = 'INCOME' AND ft.status = 'PAID' "
                + "AND ft.paid_at::date BETWEEN :from AND :to "
                + (hasFilter ? "AND ft.catalog_item_id IN (:catalogItemIds) " : "")
                + "GROUP BY 1, 2 ORDER BY 1";
        MapSqlParameterSource params = baseParams(companyId, from, to);
        if (hasFilter) {
            params.addValue("catalogItemIds", catalogItemIds);
        }
        return jdbc.query(sql, params, AVERAGE_ROW);
    }

    @Override
    public List<ProfessionalTotalsRow> revenueByProfessional(UUID companyId,
                                                             ReportGranularity granularity,
                                                             LocalDate from, LocalDate to) {
        String sql = "SELECT date_trunc('" + granularity.trunc()
                + "', ft.paid_at::timestamp)::date AS period, "
                + "ft.professional_user_id AS professional_user_id, "
                + "COALESCE(u.name, '" + UNASSIGNED_PROFESSIONAL + "') AS name, "
                + "SUM(ft.amount_cents) AS total "
                + "FROM financial_transactions ft "
                + "LEFT JOIN users u ON u.id = ft.professional_user_id "
                + "WHERE ft.company_id = :companyId AND ft.type = 'INCOME' AND ft.status = 'PAID' "
                + "AND ft.paid_at::date BETWEEN :from AND :to "
                + "GROUP BY 1, 2, 3 ORDER BY 1, 2";
        return jdbc.query(sql, baseParams(companyId, from, to), PROFESSIONAL_ROW);
    }

    @Override
    public List<UserRow> practitioners(UUID companyId, LocalDate from, LocalDate to,
                                       List<UUID> preferredIds) {
        if (preferredIds.isEmpty()) {
            String sql = "SELECT DISTINCT u.id AS id, u.name AS name "
                    + "FROM appointments a JOIN users u ON u.id = a.host_id "
                    + "WHERE a.company_id = :companyId "
                    + "AND (a.start_at AT TIME ZONE 'UTC')::date >= :from "
                    + "AND (a.start_at AT TIME ZONE 'UTC')::date <= :to "
                    + "ORDER BY u.name";
            return jdbc.query(sql, baseParams(companyId, from, to), USER_ROW);
        }
        String sql = "SELECT u.id AS id, u.name AS name "
                + "FROM users u "
                + "WHERE u.company_id = :companyId AND u.id IN (:preferredIds) "
                + "ORDER BY u.name";
        MapSqlParameterSource params = baseParams(companyId, from, to);
        params.addValue("preferredIds", preferredIds);
        return jdbc.query(sql, params, USER_ROW);
    }

    @Override
    public List<StatusPeriodCountRow> appointmentStatusByPeriod(UUID companyId,
                                                                ReportGranularity granularity,
                                                                LocalDate from, LocalDate to,
                                                                List<UUID> professionalUserIds) {
        boolean hasPros = !professionalUserIds.isEmpty();
        String sql = "SELECT date_trunc('" + granularity.trunc()
                + "', (a.start_at AT TIME ZONE 'UTC')::timestamp)::date AS period, "
                + "a.status AS status, COUNT(*) AS total "
                + "FROM appointments a "
                + "WHERE a.company_id = :companyId "
                + "AND (a.start_at AT TIME ZONE 'UTC')::date >= :from "
                + "AND (a.start_at AT TIME ZONE 'UTC')::date <= :to "
                + (hasPros ? "AND a.host_id IN (:professionalUserIds) " : "")
                + "GROUP BY 1, 2 ORDER BY 1, 2";
        return queryStatusPeriod(companyId, from, to, professionalUserIds, sql);
    }

    @Override
    public List<AgendaStatusRow> agendaByStatus(UUID companyId, LocalDate from, LocalDate to,
                                                UUID appointmentType, List<String> statuses,
                                                List<UUID> professionalUserIds) {
        boolean hasStatuses = !statuses.isEmpty();
        boolean hasPros = !professionalUserIds.isEmpty();
        String sql = "SELECT a.status AS status, COUNT(*) AS total "
                + "FROM appointments a "
                + "WHERE a.company_id = :companyId "
                + "AND (a.start_at AT TIME ZONE 'UTC')::date >= :from "
                + "AND (a.start_at AT TIME ZONE 'UTC')::date <= :to "
                + "AND (CAST(:appointmentType AS UUID) IS NULL OR a.appointment_type_id = :appointmentType) "
                + (hasStatuses ? "AND a.status IN (:statuses) " : "")
                + (hasPros ? "AND a.host_id IN (:professionalUserIds) " : "")
                + "GROUP BY a.status ORDER BY a.status";
        MapSqlParameterSource params = baseParams(companyId, from, to);
        params.addValue("appointmentType", appointmentType);
        if (hasStatuses) {
            params.addValue("statuses", statuses);
        }
        if (hasPros) {
            params.addValue("professionalUserIds", professionalUserIds);
        }
        return jdbc.query(sql, params, AGENDA_ROW);
    }

    @Override
    public List<ProcedureCountRow> procedureCountsByPeriod(UUID companyId,
                                                           ReportGranularity granularity,
                                                           LocalDate from, LocalDate to) {
        String trunc = granularity.trunc();
        String sql = String.format("""
                        SELECT period, bucket, total
                        FROM (
                            SELECT date_trunc('%s', ft.paid_at::timestamp)::date AS period,
                                   'DONE' AS bucket, COUNT(*) AS total
                            FROM financial_transactions ft
                            WHERE ft.company_id = :companyId AND ft.type = 'INCOME'
                              AND ft.status = 'PAID' AND ft.catalog_item_id IS NOT NULL
                              AND ft.paid_at::date BETWEEN :from AND :to
                            GROUP BY 1
                            UNION ALL
                            SELECT date_trunc('%s', ft.due_date::timestamp)::date, 'PENDING',
                                   COUNT(*)
                            FROM financial_transactions ft
                            WHERE ft.company_id = :companyId AND ft.type = 'INCOME'
                              AND ft.status = 'PENDING' AND ft.catalog_item_id IS NOT NULL
                              AND ft.due_date >= :from AND ft.due_date <= :to
                            GROUP BY 1
                        ) aggregation
                        ORDER BY period, bucket
                        """, trunc, trunc);
        return jdbc.query(sql, baseParams(companyId, from, to), PROCEDURE_COUNT_ROW);
    }

    @Override
    public long attendancesThisWeek(UUID companyId, LocalDate weekStart, LocalDate weekEnd) {
        String sql = "SELECT COUNT(*) AS total "
                + "FROM appointments a "
                + "WHERE a.company_id = :companyId AND a.status = 'COMPLETED' "
                + "AND (a.start_at AT TIME ZONE 'UTC')::date >= :weekStart "
                + "AND (a.start_at AT TIME ZONE 'UTC')::date <= :weekEnd";
        MapSqlParameterSource params = baseParams(companyId, weekStart, weekEnd);
        params.addValue("weekStart", weekStart);
        params.addValue("weekEnd", weekEnd);
        Long total = jdbc.queryForObject(sql, params, Long.class);
        return total == null ? 0L : total;
    }

    @Override
    public long budgetsCreatedThisMonth(UUID companyId, LocalDate monthStart,
                                        LocalDate nextMonthStart) {
        String sql = "SELECT COUNT(*) AS total "
                + "FROM opportunities o "
                + "WHERE o.company_id = :companyId "
                + "AND o.created_at >= :monthStart "
                + "AND o.created_at < :nextMonthStart";
        MapSqlParameterSource params = baseParams(companyId, monthStart, nextMonthStart);
        params.addValue("monthStart", monthStart);
        params.addValue("nextMonthStart", nextMonthStart);
        Long total = jdbc.queryForObject(sql, params, Long.class);
        return total == null ? 0L : total;
    }

    private List<StatusPeriodCountRow> queryStatusPeriod(UUID companyId, LocalDate from, LocalDate to,
                                                         List<UUID> professionalUserIds, String sql) {
        MapSqlParameterSource params = baseParams(companyId, from, to);
        if (!professionalUserIds.isEmpty()) {
            params.addValue("professionalUserIds", professionalUserIds);
        }
        return jdbc.query(sql, params, STATUS_PERIOD_ROW);
    }

    private static MapSqlParameterSource baseParams(UUID companyId, LocalDate from, LocalDate to) {
        return new MapSqlParameterSource()
                .addValue("companyId", companyId)
                .addValue("from", from)
                .addValue("to", to);
    }
}