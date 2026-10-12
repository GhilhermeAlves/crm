package com.becommerce.crm.reports.export.infrastructure.persistence;

import com.becommerce.crm.reports.export.application.port.out.ReportExportRepository;
import com.becommerce.crm.reports.export.domain.ExportFormat;
import com.becommerce.crm.reports.export.domain.ReportExport;
import com.becommerce.crm.reports.export.domain.ReportExportStatus;
import com.becommerce.crm.reports.export.domain.ReportExportType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistência JDBC de {@code report_exports} (V096). O {@code DataSource}
 * aplica as GUCs de tenant e o RLS FORCE isola a empresa; {@code company_id}
 * fica explícito nos filtros por clareza.
 */
@Repository
public class ReportExportRepositoryImpl implements ReportExportRepository {

    private static final String COLUMNS = "id, company_id, requested_by, type, format, period_from, period_to, "
            + "status, file_name, storage_object_id, error_message, created_at, finished_at";

    private static final RowMapper<ReportExport> ROW = (rs, i) -> new ReportExport(
            rs.getObject("id", UUID.class),
            rs.getObject("company_id", UUID.class),
            rs.getObject("requested_by", UUID.class),
            ReportExportType.valueOf(rs.getString("type")),
            ExportFormat.valueOf(rs.getString("format")),
            rs.getDate("period_from").toLocalDate(),
            rs.getDate("period_to").toLocalDate(),
            ReportExportStatus.valueOf(rs.getString("status")),
            rs.getString("file_name"),
            rs.getObject("storage_object_id", UUID.class),
            rs.getString("error_message"),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("finished_at")));

    private final NamedParameterJdbcTemplate jdbc;

    public ReportExportRepositoryImpl(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insert(ReportExport export) {
        String sql = "INSERT INTO report_exports (" + COLUMNS + ") VALUES (:id, :companyId, :requestedBy, "
                + ":type, :format, :periodFrom, :periodTo, :status, :fileName, :storageObjectId, "
                + ":errorMessage, :createdAt, :finishedAt)";
        jdbc.update(sql, params(export));
    }

    @Override
    public void update(ReportExport export) {
        String sql = "UPDATE report_exports SET status = :status, file_name = :fileName, "
                + "storage_object_id = :storageObjectId, error_message = :errorMessage, "
                + "finished_at = :finishedAt WHERE id = :id AND company_id = :companyId";
        jdbc.update(sql, params(export));
    }

    @Override
    public Optional<ReportExport> findById(UUID companyId, UUID id) {
        String sql = "SELECT " + COLUMNS + " FROM report_exports WHERE id = :id AND company_id = :companyId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("companyId", companyId);
        return jdbc.query(sql, params, ROW).stream().findFirst();
    }

    @Override
    public List<ReportExport> findRecent(UUID companyId, UUID requestedBy, int limit) {
        String sql = "SELECT " + COLUMNS + " FROM report_exports "
                + "WHERE company_id = :companyId AND requested_by = :requestedBy "
                + "ORDER BY created_at DESC LIMIT :limit";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("companyId", companyId)
                .addValue("requestedBy", requestedBy)
                .addValue("limit", limit);
        return jdbc.query(sql, params, ROW);
    }

    private static MapSqlParameterSource params(ReportExport export) {
        return new MapSqlParameterSource()
                .addValue("id", export.id())
                .addValue("companyId", export.companyId())
                .addValue("requestedBy", export.requestedBy())
                .addValue("type", export.type().name())
                .addValue("format", export.format().name())
                .addValue("periodFrom", export.periodFrom())
                .addValue("periodTo", export.periodTo())
                .addValue("status", export.status().name())
                .addValue("fileName", export.fileName())
                .addValue("storageObjectId", export.storageObjectId())
                .addValue("errorMessage", export.errorMessage())
                .addValue("createdAt", toTimestamp(export.createdAt()))
                .addValue("finishedAt", toTimestamp(export.finishedAt()));
    }

    private static Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
