package com.becommerce.crm.masterdata.importing.infrastructure;

import com.becommerce.crm.masterdata.importing.application.dto.DataImportSummary;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.util.*;

/** Acesso JDBC às tabelas de importação. Chamadores definem TenantContext (RLS). */
@Repository
public class DataImportRepository {

    public record ExistingKeys(Set<String> cpfs, Set<String> recordNumbers, Set<String> emails) {}

    private final NamedParameterJdbcTemplate jdbc;

    public DataImportRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public ExistingKeys existingKeys(UUID companyId) {
        Set<String> cpfs = new HashSet<>(), records = new HashSet<>(), emails = new HashSet<>();
        jdbc.query("""
                SELECT cpf, record_number, lower(email) AS email FROM contacts
                WHERE company_id = :c AND deleted_at IS NULL""", Map.of("c", companyId), rs -> {
            add(cpfs, rs.getString("cpf") == null ? null : rs.getString("cpf").replaceAll("\\D", ""));
            add(records, rs.getString("record_number"));
            add(emails, rs.getString("email"));
        });
        return new ExistingKeys(cpfs, records, emails);
    }

    private static void add(Set<String> set, String v) {
        if (v != null && !v.isBlank()) set.add(v.trim());
    }

    public UUID createImport(UUID companyId, UUID userId, String kind, String sourceFile) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO data_import (id, company_id, user_id, kind, source_file)
                VALUES (:id, :c, :u, :k, :f)""",
                new MapSqlParameterSource().addValue("id", id).addValue("c", companyId)
                        .addValue("u", userId).addValue("k", kind).addValue("f", sourceFile));
        return id;
    }

    public void linkContact(UUID companyId, UUID importId, UUID contactId, String sourceId) {
        jdbc.update("""
                INSERT INTO data_import_contacts (import_id, contact_id, company_id, source_id)
                VALUES (:i, :ct, :c, :s)""",
                new MapSqlParameterSource().addValue("i", importId).addValue("ct", contactId)
                        .addValue("c", companyId).addValue("s", sourceId));
    }

    /** Código do paciente no sistema de origem → contato, só de importações não desfeitas. */
    public Map<String, UUID> contactsBySourceId(UUID companyId) {
        Map<String, UUID> out = new HashMap<>();
        jdbc.query("""
                SELECT l.source_id, l.contact_id FROM data_import_contacts l
                JOIN data_import d ON d.id = l.import_id AND d.undone_at IS NULL
                JOIN contacts c ON c.id = l.contact_id AND c.deleted_at IS NULL
                WHERE l.company_id = :c AND l.source_id IS NOT NULL
                ORDER BY d.created_at""", Map.of("c", companyId),
                rs -> { out.put(rs.getString("source_id"), rs.getObject("contact_id", UUID.class)); });
        return out;
    }

    public String kindOf(UUID companyId, UUID importId) {
        List<String> kinds = jdbc.queryForList(
                "SELECT kind FROM data_import WHERE id = :id AND company_id = :c",
                Map.of("id", importId, "c", companyId), String.class);
        return kinds.isEmpty() ? null : kinds.get(0);
    }

    public void finishImport(UUID companyId, UUID importId, int total, int imported, int skipped, int failed) {
        jdbc.update("""
                UPDATE data_import SET total = :t, imported = :im, skipped = :s, failed = :f
                WHERE id = :id AND company_id = :c""",
                Map.of("t", total, "im", imported, "s", skipped, "f", failed, "id", importId, "c", companyId));
    }

    public List<DataImportSummary> list(UUID companyId) {
        return jdbc.query("""
                SELECT id, kind, source_file, total, imported, skipped, failed, created_at, undone_at
                FROM data_import WHERE company_id = :c ORDER BY created_at DESC LIMIT 50""",
                Map.of("c", companyId), (rs, i) -> new DataImportSummary(
                        rs.getObject("id", UUID.class), rs.getString("kind"), rs.getString("source_file"),
                        rs.getInt("total"), rs.getInt("imported"), rs.getInt("skipped"), rs.getInt("failed"),
                        rs.getTimestamp("created_at").toInstant(),
                        Optional.ofNullable(rs.getTimestamp("undone_at")).map(Timestamp::toInstant).orElse(null)));
    }

    public boolean isOpen(UUID companyId, UUID importId) {
        Integer n = jdbc.queryForObject("""
                SELECT count(*) FROM data_import WHERE id = :id AND company_id = :c AND undone_at IS NULL""",
                Map.of("id", importId, "c", companyId), Integer.class);
        return n != null && n > 0;
    }

    public List<UUID> linkedContacts(UUID companyId, UUID importId) {
        return jdbc.queryForList("""
                SELECT l.contact_id FROM data_import_contacts l
                JOIN contacts c ON c.id = l.contact_id AND c.deleted_at IS NULL
                WHERE l.import_id = :i AND l.company_id = :c""",
                Map.of("i", importId, "c", companyId), UUID.class);
    }

    public boolean hasClinicalHistory(UUID companyId, UUID contactId) {
        Boolean b = jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM appointments WHERE contact_id = :ct AND company_id = :c)
                    OR EXISTS (SELECT 1 FROM patient_procedures WHERE contact_id = :ct AND company_id = :c)""",
                Map.of("ct", contactId, "c", companyId), Boolean.class);
        return Boolean.TRUE.equals(b);
    }

    public void markUndone(UUID companyId, UUID importId) {
        jdbc.update("UPDATE data_import SET undone_at = now() WHERE id = :id AND company_id = :c",
                Map.of("id", importId, "c", companyId));
    }
}
