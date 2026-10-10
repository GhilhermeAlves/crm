package com.becommerce.crm.finance.accounting.infrastructure.persistence;

import com.becommerce.crm.finance.accounting.application.port.output.AccountingReportGateway;
import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
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
 * SELECTs nativos dos relatórios contábeis e das resoluções da importação
 * (Etapa 3). O {@code NamedParameterJdbcTemplate} usa o {@code DataSource}
 * embrulhado pelo {@code TenantDataSourcePostProcessor} (GUCs de tenant aplicadas
 * por statement) + RLS FORCE (V092)/(V093); {@code company_id} é mantido
 * explícito nos filtros por clareza.
 */
@Repository
public class AccountingReportGatewayImpl implements AccountingReportGateway {

    private static final RowMapper<CategoryTotalRow> CATEGORY_ROW = (rs, i) -> new CategoryTotalRow(
            TransactionType.valueOf(rs.getString("type")),
            TransactionCategory.valueOf(rs.getString("category")),
            rs.getLong("total"),
            rs.getLong("fee"));

    private static final RowMapper<StatementRow> STATEMENT_ROW = (rs, i) -> new StatementRow(
            rs.getDate("paid_at").toLocalDate(),
            rs.getString("description"),
            TransactionCategory.valueOf(rs.getString("category")),
            TransactionType.valueOf(rs.getString("type")),
            rs.getLong("amount_cents"));

    private static final RowMapper<TransactionDetailRow> TRANSACTION_ROW = (rs, i) -> new TransactionDetailRow(
            rs.getDate("due_date").toLocalDate(),
            toLocalDate(rs.getDate("paid_at")),
            rs.getString("description"),
            TransactionCategory.valueOf(rs.getString("category")),
            TransactionType.valueOf(rs.getString("type")),
            rs.getLong("amount_cents"),
            rs.getLong("fee_cents"),
            PaymentMethod.valueOf(rs.getString("payment_method")),
            TransactionStatus.valueOf(rs.getString("status")),
            (Integer) rs.getObject("installment_number"),
            (Integer) rs.getObject("installment_total"),
            rs.getString("contact_name"),
            rs.getString("professional_name"));

    private static final RowMapper<String> MONTH_ROW = (rs, i) -> rs.getString("month");

    private final NamedParameterJdbcTemplate jdbc;

    public AccountingReportGatewayImpl(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String companyName(UUID companyId) {
        String sql = "SELECT COALESCE(NULLIF(trading_name, ''), legal_name) AS name "
                + "FROM companies WHERE id = :companyId";
        List<String> names = jdbc.query(sql, new MapSqlParameterSource("companyId", companyId),
                (rs, i) -> rs.getString("name"));
        return names.isEmpty() ? "" : names.get(0);
    }

    @Override
    public List<CategoryTotalRow> monthlyAccounting(UUID companyId, LocalDate monthStart, LocalDate monthEnd) {
        String sql = "SELECT ft.type AS type, ft.category AS category, "
                + "SUM(ft.amount_cents) AS total, SUM(ft.fee_cents) AS fee "
                + "FROM financial_transactions ft "
                + "WHERE ft.company_id = :companyId AND ft.status = 'PAID' "
                + "AND ft.paid_at::date BETWEEN :monthStart AND :monthEnd "
                + "GROUP BY ft.type, ft.category ORDER BY ft.type, ft.category";
        return jdbc.query(sql, monthParams(companyId, monthStart, monthEnd), CATEGORY_ROW);
    }

    @Override
    public List<StatementRow> statement(UUID companyId, LocalDate monthStart, LocalDate monthEnd) {
        String sql = "SELECT ft.paid_at::date AS paid_at, ft.description AS description, "
                + "ft.category AS category, ft.type AS type, ft.amount_cents AS amount_cents "
                + "FROM financial_transactions ft "
                + "WHERE ft.company_id = :companyId AND ft.status = 'PAID' "
                + "AND ft.paid_at::date BETWEEN :monthStart AND :monthEnd "
                + "ORDER BY ft.paid_at, ft.created_at, ft.id";
        return jdbc.query(sql, monthParams(companyId, monthStart, monthEnd), STATEMENT_ROW);
    }

    @Override
    public List<TransactionDetailRow> transactionDetails(UUID companyId, LocalDate monthStart,
                                                         LocalDate monthEnd) {
        String sql = "SELECT ft.due_date AS due_date, ft.paid_at AS paid_at, ft.description AS description, "
                + "ft.category AS category, ft.type AS type, ft.amount_cents AS amount_cents, "
                + "ft.fee_cents AS fee_cents, ft.payment_method AS payment_method, ft.status AS status, "
                + "ft.installment_number AS installment_number, ft.installment_total AS installment_total, "
                + "COALESCE(c.first_name || ' ' || c.last_name, '') AS contact_name, "
                + "COALESCE(u.name, '') AS professional_name "
                + "FROM financial_transactions ft "
                + "LEFT JOIN contacts c ON c.id = ft.contact_id AND c.deleted_at IS NULL "
                + "LEFT JOIN users u ON u.id = ft.professional_user_id "
                + "WHERE ft.company_id = :companyId AND ft.status <> 'CANCELLED' "
                + "AND ft.due_date BETWEEN :monthStart AND :monthEnd "
                + "ORDER BY ft.due_date, ft.created_at, ft.id";
        return jdbc.query(sql, monthParams(companyId, monthStart, monthEnd), TRANSACTION_ROW);
    }

    @Override
    public List<String> availableMonths(UUID companyId) {
        String sql = "SELECT month FROM ("
                + "SELECT to_char(ft.due_date, 'YYYY-MM') AS month "
                + "FROM financial_transactions ft "
                + "WHERE ft.company_id = :companyId AND ft.status <> 'CANCELLED' "
                + "UNION "
                + "SELECT to_char(ft.paid_at, 'YYYY-MM') AS month "
                + "FROM financial_transactions ft "
                + "WHERE ft.company_id = :companyId AND ft.status <> 'CANCELLED' "
                + ") months ORDER BY month DESC";
        return jdbc.query(sql, new MapSqlParameterSource("companyId", companyId), MONTH_ROW);
    }

    @Override
    public UUID resolveContact(UUID companyId, String fullName) {
        String sql = "SELECT id FROM contacts "
                + "WHERE company_id = :companyId AND deleted_at IS NULL "
                + "AND LOWER(TRIM(COALESCE(first_name, '') || ' ' || COALESCE(last_name, ''))) = LOWER(:name) "
                + "LIMIT 1";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("companyId", companyId)
                .addValue("name", fullName);
        return queryOptionalUuid(sql, params);
    }

    @Override
    public UUID resolveProfessional(UUID companyId, String nameOrEmail) {
        String sql = "SELECT id FROM users "
                + "WHERE company_id = :companyId AND deleted_at IS NULL "
                + "AND (LOWER(name) = LOWER(:nameOrEmail) OR LOWER(email) = LOWER(:nameOrEmail)) "
                + "LIMIT 1";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("companyId", companyId)
                .addValue("nameOrEmail", nameOrEmail);
        return queryOptionalUuid(sql, params);
    }

    private UUID queryOptionalUuid(String sql, MapSqlParameterSource params) {
        return jdbc.query(sql, params, rs -> rs.next() ? rs.getObject(1, UUID.class) : null);
    }

    private static MapSqlParameterSource monthParams(UUID companyId, LocalDate monthStart, LocalDate monthEnd) {
        return new MapSqlParameterSource()
                .addValue("companyId", companyId)
                .addValue("monthStart", monthStart)
                .addValue("monthEnd", monthEnd);
    }

    private static LocalDate toLocalDate(java.sql.Date date) {
        return date == null ? null : date.toLocalDate();
    }
}