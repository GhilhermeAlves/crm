package com.becommerce.crm.sales.scheduling.infrastructure.persistence;

import com.becommerce.crm.sales.scheduling.application.dto.ProcedureRef;
import com.becommerce.crm.sales.scheduling.application.port.out.AppointmentProcedureRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * JDBC simples sobre appointment_procedures. O isolamento por empresa vem do RLS
 * (TenantAwareDataSource aplica o tenant da transação na conexão).
 */
@Repository
public class AppointmentProcedureRepositoryImpl implements AppointmentProcedureRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AppointmentProcedureRepositoryImpl(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<UUID, List<ProcedureRef>> findProcedures(Collection<UUID> appointmentIds) {
        if (appointmentIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<ProcedureRef>> result = new HashMap<>();
        jdbc.query("SELECT ap.appointment_id, ci.id, ci.name FROM appointment_procedures ap "
                        + "JOIN catalog_items ci ON ci.id = ap.catalog_item_id "
                        + "WHERE ap.appointment_id IN (:ids) ORDER BY ci.name, ci.id",
                Map.of("ids", appointmentIds),
                rs -> {
                    result.computeIfAbsent(rs.getObject("appointment_id", UUID.class), k -> new ArrayList<>())
                            .add(new ProcedureRef(rs.getObject("id", UUID.class), rs.getString("name")));
                });
        return result;
    }

    @Override
    public void replace(UUID companyId, UUID appointmentId, Collection<UUID> catalogItemIds) {
        jdbc.update("DELETE FROM appointment_procedures WHERE appointment_id = :appointmentId",
                Map.of("appointmentId", appointmentId));
        if (catalogItemIds.isEmpty()) {
            return;
        }
        MapSqlParameterSource[] rows = new LinkedHashSet<>(catalogItemIds).stream()
                .map(itemId -> new MapSqlParameterSource()
                        .addValue("appointmentId", appointmentId)
                        .addValue("itemId", itemId)
                        .addValue("companyId", companyId))
                .toArray(MapSqlParameterSource[]::new);
        jdbc.batchUpdate("INSERT INTO appointment_procedures (appointment_id, catalog_item_id, company_id) "
                + "VALUES (:appointmentId, :itemId, :companyId)", rows);
    }
}
