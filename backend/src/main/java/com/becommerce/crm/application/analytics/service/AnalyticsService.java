package com.becommerce.crm.application.analytics.service;

import com.becommerce.crm.application.analytics.AnalyticsPeriod;
import com.becommerce.crm.application.analytics.dto.AnalyticsSummaryResponse;
import com.becommerce.crm.application.analytics.port.input.AnalyticsUseCase;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Analytics read-only (Sprint 19). Todas as métricas são AGREGAÇÕES SQL sobre
 * as tabelas operacionais existentes (sem N+1, sem cópia de dados), sempre com
 * filtro explícito de {@code company_id} + RLS FORCE + {@link TenantContext}.
 *
 * <p>Períodos: início inclusivo, fim exclusivo; comparação temporal contra o
 * período imediatamente anterior de mesma duração.
 */
@Service
public class AnalyticsService implements AnalyticsUseCase {

    private final NamedParameterJdbcTemplate jdbc;

    public AnalyticsService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse summary(UUID companyId, AnalyticsPeriod period) {
        try {
            TenantContext.setCompanyId(companyId);
            var current = collectMetrics(companyId, period);
            var previous = collectMetrics(companyId, period.previous());
            var series = dailySeries(companyId, period);
            return new AnalyticsSummaryResponse(
                    period.from().toLocalDate().toString(),
                    period.to().toLocalDate().minusDays(1).toString(),
                    current, previous, series);
        } finally {
            TenantContext.clear();
        }
    }

    private AnalyticsSummaryResponse.Metrics collectMetrics(UUID companyId, AnalyticsPeriod p) {
        MapSqlParameterScope scope = new MapSqlParameterScope(companyId, p);

        long contactsCreated = count(scope.forTable("contacts", "created_at"));
        long leadsCreated = count(scope.forTable("leads", "created_at"));
        long leadsConverted = count(scope
                .forTable("leads", "updated_at")
                .addValue("extraWhere", " AND status = 'CONVERTED' "));
        long opportunitiesCreated = count(scope.forTable("opportunities", "created_at"));
        long opportunitiesWon = count(scope
                .forTable("opportunities", "won_at")
                .addValue("extraWhere", " AND status = 'WON' "));

        BigDecimal wonValue = queryDecimal(
                "SELECT COALESCE(SUM(value), 0) FROM opportunities " +
                        "WHERE company_id = :companyId AND status = 'WON' " +
                        "AND won_at >= :from AND won_at < :to", scope.base());
        BigDecimal pipelineOpenValue = queryDecimal(
                "SELECT COALESCE(SUM(value), 0) FROM opportunities " +
                        "WHERE company_id = :companyId AND status = 'OPEN'", scope.base());

        long activitiesCreated = count(scope.forTable("activities", "created_at"));
        long tasksCreated = count(scope.forTable("tasks", "created_at"));
        long tasksCompleted = count(scope
                .forTable("tasks", "updated_at")
                .addValue("extraWhere", " AND status = 'COMPLETED' "));
        long tasksOverdue = queryLong(
                "SELECT COUNT(*) FROM tasks WHERE company_id = :companyId " +
                        "AND status IN ('PENDING','IN_PROGRESS') " +
                        "AND due_at IS NOT NULL AND due_at < NOW()", scope.base());

        long campaignsExecuted = count(scope
                .forTable("campaign_executions", "finished_at")
                .addValue("extraWhere", " AND status = 'COMPLETED' "));
        long campaignMessagesSent = count(scope
                .forTable("campaign_message_events", "occurred_at")
                .addValue("extraWhere", " AND status IN ('SENT','DELIVERED','READ') "));
        long campaignMessagesFailed = count(scope
                .forTable("campaign_message_events", "occurred_at")
                .addValue("extraWhere", " AND status = 'FAILED' "));

        long messagesIn = count(scope
                .forTable("omnichannel_messages", "created_at")
                .addValue("extraWhere", " AND direction = 'INBOUND' "));
        long messagesOut = count(scope
                .forTable("omnichannel_messages", "created_at")
                .addValue("extraWhere", " AND direction = 'OUTBOUND' "));

        long workflowRunsMatched = count(scope
                .forTable("workflow_runs", "created_at"));
        long workflowRunsSuccess = count(scope
                .forTable("workflow_runs", "created_at")
                .addValue("extraWhere", " AND status = 'SUCCESS' "));
        long workflowRunsFailed = count(scope
                .forTable("workflow_runs", "created_at")
                .addValue("extraWhere", " AND status = 'FAILED' "));

        return new AnalyticsSummaryResponse.Metrics(
                contactsCreated, leadsCreated, leadsConverted,
                opportunitiesCreated, opportunitiesWon, wonValue, pipelineOpenValue,
                activitiesCreated, tasksCreated, tasksCompleted, tasksOverdue,
                campaignsExecuted, campaignMessagesSent, campaignMessagesFailed,
                messagesIn, messagesOut,
                workflowRunsMatched, workflowRunsSuccess, workflowRunsFailed);
    }

    private List<AnalyticsSummaryResponse.DailyPoint> dailySeries(UUID companyId, AnalyticsPeriod p) {
        String sql = """
                WITH days AS (
                    SELECT generate_series(CAST(:from AS TIMESTAMP), CAST(:to AS TIMESTAMP),
                                           INTERVAL '1 day')::date::text AS day
                ),
                leads_daily AS (
                    SELECT created_at::date::text AS day, COUNT(*) AS cnt
                      FROM leads WHERE company_id = :companyId
                       AND created_at >= :from AND created_at < :to
                     GROUP BY 1
                ),
                opps_daily AS (
                    SELECT created_at::date::text AS day, COUNT(*) AS cnt
                      FROM opportunities WHERE company_id = :companyId
                       AND created_at >= :from AND created_at < :to
                     GROUP BY 1
                ),
                msgs_daily AS (
                    SELECT created_at::date::text AS day, COUNT(*) AS cnt
                      FROM omnichannel_messages WHERE company_id = :companyId
                       AND direction = 'OUTBOUND'
                       AND created_at >= :from AND created_at < :to
                     GROUP BY 1
                )
                SELECT d.day,
                       COALESCE(l.cnt, 0) AS leads,
                       COALESCE(o.cnt, 0) AS opportunities,
                       COALESCE(m.cnt, 0) AS messages_sent
                  FROM days d
                  LEFT JOIN leads_daily l ON l.day = d.day
                  LEFT JOIN opps_daily o ON o.day = d.day
                  LEFT JOIN msgs_daily m ON m.day = d.day
                 ORDER BY d.day
                """;
        return jdbc.query(sql,
                new MapSqlParameterSource()
                        .addValue("from", p.from())
                        .addValue("to", p.to())
                        .addValue("companyId", companyId),
                (rs, n) -> new AnalyticsSummaryResponse.DailyPoint(
                        rs.getString("day"), rs.getLong("leads"),
                        rs.getLong("opportunities"), rs.getLong("messages_sent")));
    }

    private long count(MapSqlParameterSource params) {
        Long value = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + params.getValue("table") + " " +
                        "WHERE company_id = :companyId " +
                        "AND " + params.getValue("column") + " >= :from " +
                        "AND " + params.getValue("column") + " < :to" +
                        params.getValue("extraWhere"),
                params, Long.class);
        return value != null ? value : 0L;
    }

    private BigDecimal queryDecimal(String sql, MapSqlParameterSource params) {
        BigDecimal value = jdbc.queryForObject(sql, params, BigDecimal.class);
        return value != null ? value : BigDecimal.ZERO;
    }

    private long queryLong(String sql, MapSqlParameterSource params) {
        Long value = jdbc.queryForObject(sql, params, Long.class);
        return value != null ? value : 0L;
    }

    /** Helper interno que monta os parâmetros padrão de contagem por tabela. */
    private static final class MapSqlParameterScope {
        private final UUID companyId;
        private final AnalyticsPeriod period;

        private MapSqlParameterScope(UUID companyId, AnalyticsPeriod period) {
            this.companyId = companyId;
            this.period = period;
        }

        MapSqlParameterSource base() {
            return new MapSqlParameterSource()
                    .addValue("companyId", companyId)
                    .addValue("from", period.from())
                    .addValue("to", period.to());
        }

        MapSqlParameterSource forTable(String table, String column) {
            return base()
                    .addValue("table", table)
                    .addValue("column", column)
                    .addValue("extraWhere", "");
        }
    }
}
