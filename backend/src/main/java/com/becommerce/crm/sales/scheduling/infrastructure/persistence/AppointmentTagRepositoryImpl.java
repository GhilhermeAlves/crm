package com.becommerce.crm.sales.scheduling.infrastructure.persistence;

import com.becommerce.crm.sales.scheduling.application.port.out.AppointmentTagRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * JDBC simples sobre appointment_tags. O isolamento por empresa vem do RLS
 * (TenantAwareDataSource aplica o tenant da transação na conexão).
 */
@Repository
public class AppointmentTagRepositoryImpl implements AppointmentTagRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AppointmentTagRepositoryImpl(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<UUID, List<UUID>> findTagIds(Collection<UUID> appointmentIds) {
        if (appointmentIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<UUID>> result = new HashMap<>();
        jdbc.query("SELECT appointment_id, tag_id FROM appointment_tags WHERE appointment_id IN (:ids) "
                        + "ORDER BY created_at, tag_id",
                Map.of("ids", appointmentIds),
                rs -> {
                    result.computeIfAbsent(rs.getObject("appointment_id", UUID.class), k -> new ArrayList<>())
                            .add(rs.getObject("tag_id", UUID.class));
                });
        return result;
    }

    @Override
    public void replace(UUID companyId, UUID appointmentId, Collection<UUID> tagIds) {
        jdbc.update("DELETE FROM appointment_tags WHERE appointment_id = :appointmentId",
                Map.of("appointmentId", appointmentId));
        if (tagIds.isEmpty()) {
            return;
        }
        MapSqlParameterSource[] rows = new LinkedHashSet<>(tagIds).stream()
                .map(tagId -> new MapSqlParameterSource()
                        .addValue("appointmentId", appointmentId)
                        .addValue("tagId", tagId)
                        .addValue("companyId", companyId))
                .toArray(MapSqlParameterSource[]::new);
        jdbc.batchUpdate("INSERT INTO appointment_tags (appointment_id, tag_id, company_id) "
                + "VALUES (:appointmentId, :tagId, :companyId)", rows);
    }
}
