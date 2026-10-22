package com.becommerce.crm.masterdata.company.infrastructure.persistence;

import com.becommerce.crm.masterdata.company.application.port.output.CompanyModulesRepository;
import com.becommerce.crm.masterdata.company.domain.CompanyModules;
import com.becommerce.crm.masterdata.company.domain.CompanySegment;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Lê/grava apenas {@code segment} e {@code enabled_modules} de {@code companies}
 * (tabela sem RLS). Separado do agregado {@link CompanyJpaEntity} para que o
 * PUT comum de empresa nunca altere módulos por acidente.
 */
@Repository
public class CompanyModulesJdbcRepository implements CompanyModulesRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public CompanyModulesJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<CompanyModules> findByCompanyId(UUID companyId) {
        return jdbc.query(
                "SELECT segment, enabled_modules FROM companies WHERE id = :id",
                new MapSqlParameterSource("id", companyId),
                (rs, i) -> new CompanyModules(
                        parseSegment(rs.getString("segment")),
                        CompanyModules.parseCsv(rs.getString("enabled_modules"))))
                .stream().findFirst();
    }

    @Override
    public boolean update(UUID companyId, CompanyModules modules) {
        int rows = jdbc.update(
                "UPDATE companies SET segment = :segment, enabled_modules = :modules, "
                        + "updated_at = CURRENT_TIMESTAMP WHERE id = :id",
                new MapSqlParameterSource()
                        .addValue("id", companyId)
                        .addValue("segment", modules.segment().name())
                        .addValue("modules", modules.toCsv()));
        return rows > 0;
    }

    private static CompanySegment parseSegment(String value) {
        try {
            return value == null ? CompanySegment.GENERIC : CompanySegment.valueOf(value);
        } catch (IllegalArgumentException e) {
            return CompanySegment.GENERIC;
        }
    }
}
