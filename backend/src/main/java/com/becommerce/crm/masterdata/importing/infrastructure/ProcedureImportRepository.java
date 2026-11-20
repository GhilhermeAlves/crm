package com.becommerce.crm.masterdata.importing.infrastructure;

import com.becommerce.crm.masterdata.importing.application.ProcedureRowMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.*;

/**
 * Gravação direta (JDBC) dos procedimentos importados: não passa pelo service da
 * ficha clínica para não gerar lançamento financeiro nem automações. Chamadores
 * definem TenantContext (RLS).
 */
@Repository
public class ProcedureImportRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ProcedureImportRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Serviços ativos do catálogo: nome normalizado → id. */
    public Map<String, UUID> servicesByName(UUID companyId) {
        Map<String, UUID> out = new HashMap<>();
        jdbc.query("""
                SELECT id, name FROM catalog_items
                WHERE company_id = :c AND item_type = 'SERVICE' AND active = TRUE
                ORDER BY created_at""", Map.of("c", companyId),
                rs -> { out.putIfAbsent(normalize(rs.getString("name")), rs.getObject("id", UUID.class)); });
        return out;
    }

    /** Procedimentos de origem já importados em importações não desfeitas. */
    public Set<String> importedSourceIds(UUID companyId) {
        return new HashSet<>(jdbc.queryForList("""
                SELECT l.source_id FROM data_import_procedures l
                JOIN data_import d ON d.id = l.import_id AND d.undone_at IS NULL
                WHERE l.company_id = :c AND l.source_id IS NOT NULL""", Map.of("c", companyId), String.class));
    }

    public UUID createService(UUID companyId, UUID importId, String name, long priceCents) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO catalog_items (id, company_id, item_type, name, price, active)
                VALUES (:id, :c, 'SERVICE', :n, :p, TRUE)""",
                new MapSqlParameterSource().addValue("id", id).addValue("c", companyId).addValue("n", name)
                        .addValue("p", BigDecimal.valueOf(priceCents, 2)));
        jdbc.update("""
                INSERT INTO data_import_catalog_items (import_id, catalog_item_id, company_id)
                VALUES (:i, :id, :c)""", Map.of("i", importId, "id", id, "c", companyId));
        return id;
    }

    public void insertProcedure(UUID companyId, UUID importId, UUID userId, UUID contactId, UUID catalogItemId,
                                String name, ProcedureRowMapper.Mapped p) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO patient_procedures (id, company_id, contact_id, catalog_item_id, name_snapshot, state,
                    dentition, price_cents, notes, performed_at, created_by)
                VALUES (:id, :c, :ct, :ci, :n, :s, :d, :pc, :notes, :pa, :u)""",
                new MapSqlParameterSource().addValue("id", id).addValue("c", companyId).addValue("ct", contactId)
                        .addValue("ci", catalogItemId).addValue("n", name).addValue("s", p.state())
                        .addValue("d", p.dentition()).addValue("pc", p.priceCents()).addValue("notes", p.notes())
                        .addValue("pa", p.performedAt() == null ? null : Date.valueOf(p.performedAt()))
                        .addValue("u", userId));
        for (ProcedureRowMapper.Tooth t : p.teeth()) {
            jdbc.update("""
                    INSERT INTO patient_procedure_teeth (procedure_id, company_id, tooth, faces)
                    VALUES (:id, :c, :t, CAST(:f AS VARCHAR(1)[]))""",
                    Map.of("id", id, "c", companyId, "t", t.number(), "f", "{" + String.join(",", t.faces()) + "}"));
        }
        jdbc.update("""
                INSERT INTO data_import_procedures (import_id, procedure_id, company_id, source_id)
                VALUES (:i, :id, :c, :s)""",
                new MapSqlParameterSource().addValue("i", importId).addValue("id", id).addValue("c", companyId)
                        .addValue("s", p.sourceId()));
    }

    /** Remove os procedimentos da importação; devolve quantos saíram. */
    public int deleteProcedures(UUID companyId, UUID importId) {
        return jdbc.update("""
                DELETE FROM patient_procedures WHERE company_id = :c AND id IN
                    (SELECT procedure_id FROM data_import_procedures WHERE import_id = :i AND company_id = :c)""",
                Map.of("c", companyId, "i", importId));
    }

    /** Remove os serviços criados pela importação que ninguém usa; os em uso ficam. */
    public int deleteUnusedServices(UUID companyId, UUID importId) {
        List<UUID> ids = jdbc.queryForList("""
                SELECT catalog_item_id FROM data_import_catalog_items WHERE import_id = :i AND company_id = :c""",
                Map.of("i", importId, "c", companyId), UUID.class);
        int removed = 0;
        for (UUID id : ids) {
            try {
                removed += jdbc.update("""
                        DELETE FROM catalog_items WHERE id = :id AND company_id = :c
                          AND NOT EXISTS (SELECT 1 FROM patient_procedures WHERE catalog_item_id = :id)""",
                        Map.of("id", id, "c", companyId));
            } catch (DataIntegrityViolationException e) {
                // usado em orçamento/agenda: mantém
            }
        }
        return removed;
    }

    public static String normalize(String name) {
        return name == null ? "" : name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
