package com.becommerce.crm.finance.accounting.infrastructure.persistence;

import com.becommerce.crm.finance.accounting.application.port.output.AccountingReportRequestRepository;
import com.becommerce.crm.finance.accounting.domain.AccountingReportType;
import com.becommerce.crm.finance.accounting.domain.ReportFormat;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.UUID;

/**
 * Persistência de {@code accounting_report_requests} (V093): upsert por
 * (empresa, tipo, formato, mês) — a regra da tela é auditoria, não bloqueio,
 * então o reenvio incrementa {@code generated_count} em vez de rejeitar. RLS
 * FORCE isola o tenant.
 */
@Repository
public class AccountingReportRequestRepositoryImpl implements AccountingReportRequestRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AccountingReportRequestRepositoryImpl(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public int register(UUID companyId, AccountingReportType type, ReportFormat format, YearMonth month,
                        UUID requestedBy) {
        String sql = "INSERT INTO accounting_report_requests (company_id, type, format, month, requested_by) "
                + "VALUES (:companyId, :type, :format, :month, :requestedBy) "
                + "ON CONFLICT (company_id, type, format, month) DO UPDATE SET "
                + "generated_count = accounting_report_requests.generated_count + 1, "
                + "requested_at = now(), "
                + "requested_by = EXCLUDED.requested_by "
                + "RETURNING generated_count";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("companyId", companyId)
                .addValue("type", type.name())
                .addValue("format", format.name())
                .addValue("month", month.toString())
                .addValue("requestedBy", requestedBy);
        Integer count = jdbc.queryForObject(sql, params, Integer.class);
        return count == null ? 1 : count;
    }
}