package com.becommerce.crm.reports.export.infrastructure.persistence;

import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionStatus;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SELECTs nativos dos relatórios exportados. O {@code DataSource} aplica as
 * GUCs de tenant (RLS FORCE); {@code company_id} fica explícito nos filtros.
 * Datas da agenda são convertidas para o horário de Brasília.
 */
@Repository
public class ReportDataGatewayImpl implements ReportDataGateway {

    private static final RowMapper<CategoryTotalRow> CATEGORY_ROW = (rs, i) -> new CategoryTotalRow(
            TransactionCategory.valueOf(rs.getString("category")),
            rs.getLong("total_count"),
            rs.getLong("total"),
            rs.getLong("fee"),
            rs.getLong("fee_count"));

    private static final RowMapper<DailyTotalRow> DAILY_ROW = (rs, i) -> new DailyTotalRow(
            rs.getDate("day").toLocalDate(),
            TransactionType.valueOf(rs.getString("type")),
            rs.getLong("total"));

    private static final RowMapper<TransactionRow> TRANSACTION_ROW = (rs, i) -> new TransactionRow(
            rs.getDate("due_date").toLocalDate(),
            toLocalDate(rs.getDate("paid_at")),
            rs.getString("description"),
            rs.getString("contact_name"),
            TransactionCategory.valueOf(rs.getString("category")),
            PaymentMethod.valueOf(rs.getString("payment_method")),
            TransactionType.valueOf(rs.getString("type")),
            rs.getLong("amount_cents"),
            TransactionStatus.valueOf(rs.getString("status")));

    private static final RowMapper<AppointmentRow> APPOINTMENT_ROW = (rs, i) -> new AppointmentRow(
            rs.getTimestamp("start_at").toInstant(),
            rs.getString("contact_name"),
            rs.getString("type_name"),
            rs.getString("host_name"),
            rs.getString("status"),
            rs.getString("source"));

    private final NamedParameterJdbcTemplate jdbc;

    public ReportDataGatewayImpl(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public CompanyHeader companyHeader(UUID companyId) {
        String sql = "SELECT COALESCE(NULLIF(trading_name, ''), legal_name) AS name, cnpj "
                + "FROM companies WHERE id = :companyId";
        List<CompanyHeader> rows = jdbc.query(sql, new MapSqlParameterSource("companyId", companyId),
                (rs, i) -> new CompanyHeader(rs.getString("name"), rs.getString("cnpj")));
        return rows.isEmpty() ? new CompanyHeader("", "") : rows.get(0);
    }

    @Override
    public List<CategoryTotalRow> paidByCategory(UUID companyId, LocalDate from, LocalDate to) {
        String sql = "SELECT ft.category AS category, COUNT(*) AS total_count, "
                + "SUM(ft.amount_cents) AS total, SUM(ft.fee_cents) AS fee, "
                + "COUNT(*) FILTER (WHERE ft.fee_cents > 0) AS fee_count "
                + "FROM financial_transactions ft "
                + "WHERE ft.company_id = :companyId AND ft.status = 'PAID' "
                + "AND ft.paid_at BETWEEN :from AND :to "
                + "GROUP BY ft.category ORDER BY ft.category";
        return jdbc.query(sql, params(companyId, from, to), CATEGORY_ROW);
    }

    @Override
    public List<DailyTotalRow> paidByDay(UUID companyId, LocalDate from, LocalDate to) {
        String sql = "SELECT ft.paid_at AS day, ft.type AS type, SUM(ft.amount_cents) AS total "
                + "FROM financial_transactions ft "
                + "WHERE ft.company_id = :companyId AND ft.status = 'PAID' "
                + "AND ft.paid_at BETWEEN :from AND :to "
                + "GROUP BY ft.paid_at, ft.type ORDER BY ft.paid_at, ft.type";
        return jdbc.query(sql, params(companyId, from, to), DAILY_ROW);
    }

    @Override
    public List<TransactionRow> transactions(UUID companyId, LocalDate from, LocalDate to) {
        String sql = "SELECT ft.due_date AS due_date, ft.paid_at AS paid_at, ft.description AS description, "
                + "COALESCE(TRIM(COALESCE(c.first_name, '') || ' ' || COALESCE(c.last_name, '')), '') "
                + "AS contact_name, "
                + "ft.category AS category, ft.payment_method AS payment_method, ft.type AS type, "
                + "ft.amount_cents AS amount_cents, ft.status AS status "
                + "FROM financial_transactions ft "
                + "LEFT JOIN contacts c ON c.id = ft.contact_id AND c.deleted_at IS NULL "
                + "WHERE ft.company_id = :companyId AND ft.status <> 'CANCELLED' "
                + "AND ft.due_date BETWEEN :from AND :to "
                + "ORDER BY ft.due_date, ft.created_at, ft.id";
        return jdbc.query(sql, params(companyId, from, to), TRANSACTION_ROW);
    }

    @Override
    public List<AppointmentRow> appointments(UUID companyId, LocalDate from, LocalDate to) {
        String sql = "SELECT a.start_at AS start_at, "
                + "COALESCE(TRIM(COALESCE(c.first_name, '') || ' ' || COALESCE(c.last_name, '')), '') "
                + "AS contact_name, "
                + "COALESCE(t.name, a.title) AS type_name, COALESCE(u.name, '') AS host_name, "
                + "a.status AS status, a.source AS source "
                + "FROM appointments a "
                + "LEFT JOIN contacts c ON c.id = a.contact_id "
                + "LEFT JOIN appointment_types t ON t.id = a.appointment_type_id "
                + "LEFT JOIN users u ON u.id = a.host_id "
                + "WHERE a.company_id = :companyId "
                + "AND (a.start_at AT TIME ZONE 'America/Sao_Paulo')::date BETWEEN :from AND :to "
                + "ORDER BY u.name, a.start_at";
        return jdbc.query(sql, params(companyId, from, to), APPOINTMENT_ROW);
    }

    private static MapSqlParameterSource params(UUID companyId, LocalDate from, LocalDate to) {
        return new MapSqlParameterSource()
                .addValue("companyId", companyId)
                .addValue("from", from)
                .addValue("to", to);
    }

    private static LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }
}
