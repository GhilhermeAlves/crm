package com.becommerce.crm.finance.report.infrastructure.persistence;

import com.becommerce.crm.finance.report.application.port.output.ReportSuggestionRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Inserção de {@code report_suggestions} (V094). As categorias são gravadas como
 * {@code text[]} do PostgreSQL; RLS FORCE isola o tenant.
 */
@Repository
public class ReportSuggestionRepositoryImpl implements ReportSuggestionRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ReportSuggestionRepositoryImpl(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UUID create(UUID companyId, List<String> categories, String notes, UUID createdBy) {
        String sql = "INSERT INTO report_suggestions (company_id, categories, notes, created_by) "
                + "VALUES (:companyId, :categories, :notes, :createdBy) RETURNING id";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("companyId", companyId)
                .addValue("categories", categories.toArray(String[]::new))
                .addValue("notes", notes)
                .addValue("createdBy", createdBy);
        return jdbc.queryForObject(sql, params, UUID.class);
    }
}
