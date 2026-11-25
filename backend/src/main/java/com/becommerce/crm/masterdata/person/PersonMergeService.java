package com.becommerce.crm.masterdata.person;

import com.becommerce.crm.masterdata.person.PersonDtos.DuplicateGroup;
import com.becommerce.crm.masterdata.person.PersonDtos.DuplicateMember;
import com.becommerce.crm.masterdata.person.PersonDtos.MergeResult;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Tela Duplicados (doc 22.8): pendências da migração (person_migration_issues) e possíveis
 * duplicados (mesmo celular ou mesmo nome + nascimento). A unificação move todas as referências
 * do cadastro removido para o mantido — inclusive as tabelas que apontam para contacts.id
 * (agenda, prontuário, orçamentos, financeiro, conversas...) — e exclui logicamente o removido.
 */
@Service
public class PersonMergeService {

    private final JdbcTemplate jdbc;
    private final PersonHistoryService history;
    private final PersonContactBridge bridge;

    public PersonMergeService(JdbcTemplate jdbc, PersonHistoryService history, PersonContactBridge bridge) {
        this.jdbc = jdbc;
        this.history = history;
        this.bridge = bridge;
    }

    @Transactional(readOnly = true)
    public List<DuplicateGroup> duplicates(UUID companyId) {
        List<DuplicateGroup> groups = new ArrayList<>();
        // 1. Pendências da migração (cada uma vira um grupo com o cadastro mantido, quando houver).
        jdbc.query("SELECT i.id AS iid, i.issue_type, i.original_value, i.person_id, i.kept_person_id FROM person_migration_issues i "
                + "JOIN persons p ON p.id = i.person_id WHERE i.company_id = ? AND i.resolved_at IS NULL AND NOT p.deleted "
                + "ORDER BY i.created_at", rs -> {
            UUID iid = rs.getObject("iid", UUID.class);
            String type = rs.getString("issue_type");
            List<DuplicateMember> m = new ArrayList<>();
            UUID kept = rs.getObject("kept_person_id", UUID.class);
            if (kept != null) m.addAll(members(companyId, List.of(kept), null, null));
            m.addAll(members(companyId, List.of(rs.getObject("person_id", UUID.class)), iid, type));
            String reason = switch (type) {
                case "INVALID_TAX_ID" -> "CPF/CNPJ inválido na migração";
                case "DUPLICATE_SAME_PERSON" -> "Mesmo CPF (mesma pessoa)";
                default -> "Mesmo CPF em pessoas diferentes";
            };
            groups.add(new DuplicateGroup(reason, TaxIdValidator.mask(TaxIdValidator.normalize(rs.getString("original_value"))), m));
        }, companyId);
        // 2. Mesmo celular.
        jdbc.query("SELECT pc.value, array_agg(DISTINCT pc.person_id) AS ids FROM person_contacts pc JOIN persons p ON p.id = pc.person_id "
                + "WHERE pc.company_id = ? AND pc.active AND pc.contact_type = 'MOBILE' AND NOT p.deleted "
                + "GROUP BY pc.value HAVING count(DISTINCT pc.person_id) > 1 LIMIT 50", rs -> {
            UUID[] ids = (UUID[]) rs.getArray("ids").getArray();
            groups.add(new DuplicateGroup("Mesmo celular", rs.getString("value"), members(companyId, List.of(ids), null, null)));
        }, companyId);
        // 3. Mesmo nome e nascimento.
        jdbc.query("SELECT app.normalize_name(name) AS n, birth_or_foundation_date AS b, array_agg(id) AS ids FROM persons "
                + "WHERE company_id = ? AND NOT deleted AND birth_or_foundation_date IS NOT NULL "
                + "GROUP BY 1, 2 HAVING count(*) > 1 LIMIT 50", rs -> {
            UUID[] ids = (UUID[]) rs.getArray("ids").getArray();
            groups.add(new DuplicateGroup("Mesmo nome e nascimento", rs.getString("n"), members(companyId, List.of(ids), null, null)));
        }, companyId);
        return groups;
    }

    private List<DuplicateMember> members(UUID companyId, List<UUID> ids, UUID issueId, String issueType) {
        List<DuplicateMember> out = new ArrayList<>();
        for (UUID id : ids) {
            jdbc.query("SELECT p.id, p.name, p.tax_id, p.birth_or_foundation_date, p.created_at, "
                    + "(SELECT value FROM person_contacts pc WHERE pc.person_id = p.id AND pc.contact_type = 'MOBILE' "
                    + " ORDER BY pc.is_primary DESC LIMIT 1) AS mobile FROM persons p WHERE p.company_id = ? AND p.id = ?",
                    rs -> {
                        out.add(new DuplicateMember(rs.getObject("id", UUID.class), rs.getString("name"),
                                TaxIdValidator.mask(rs.getString("tax_id")), rs.getObject("birth_or_foundation_date", LocalDate.class),
                                rs.getString("mobile"), rs.getObject("created_at", OffsetDateTime.class), issueId, issueType));
                    }, companyId, id);
        }
        return out;
    }

    @Transactional
    public void dismissIssue(UUID companyId, UUID issueId, CurrentUser user) {
        if (jdbc.update("UPDATE person_migration_issues SET resolved_at = now(), resolved_by = ? WHERE company_id = ? AND id = ? "
                + "AND resolved_at IS NULL", user.userId(), companyId, issueId) == 0) {
            throw PersonException.notFound("Pendência");
        }
    }

    @Transactional
    public MergeResult merge(UUID companyId, UUID keepId, UUID removeId, String reason, CurrentUser user) {
        if (keepId == null || removeId == null || keepId.equals(removeId)) {
            throw PersonException.badRequest("Escolha dois cadastros diferentes.");
        }
        if (reason == null || reason.isBlank()) throw PersonException.badRequest("Informe o motivo da unificação.");
        for (UUID id : List.of(keepId, removeId)) {
            Boolean ok = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM persons WHERE company_id = ? AND id = ? AND NOT deleted)",
                    Boolean.class, companyId, id);
            if (!Boolean.TRUE.equals(ok)) throw PersonException.notFound("Cadastro");
        }
        List<String> moved = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        // Paciente: se só o removido é cliente/paciente, o mantido assume.
        boolean removeHasContact = bridge.hasContact(companyId, removeId);
        if (removeHasContact) bridge.createContactFor(companyId, keepId);
        if (removeHasContact) {
            Map<String, Object> r = jdbc.queryForMap("SELECT is_patient, patient_since, record_number, health_plan, health_plan_number, "
                    + "referral_source FROM contacts WHERE id = ?", removeId);
            if (Boolean.TRUE.equals(r.get("is_patient"))) {
                Object rec = r.get("record_number");
                jdbc.update("UPDATE contacts SET record_number = NULL WHERE id = ?", removeId);
                jdbc.update("UPDATE contacts SET is_patient = TRUE, patient_since = coalesce(patient_since, ?), "
                                + "record_number = coalesce(record_number, ?), health_plan = coalesce(health_plan, ?), "
                                + "health_plan_number = coalesce(health_plan_number, ?), referral_source = coalesce(referral_source, ?), "
                                + "updated_at = now() WHERE id = ?",
                        r.get("patient_since"), rec, r.get("health_plan"), r.get("health_plan_number"), r.get("referral_source"), keepId);
            }
        }

        // Tabelas que apontam para contacts(id): move as referências (savepoint por tabela).
        List<Map<String, Object>> fks = jdbc.queryForList(
                "SELECT c.conrelid::regclass::text AS tbl, a.attname AS col FROM pg_constraint c "
                        + "JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = c.conkey[1] "
                        + "WHERE c.contype = 'f' AND c.confrelid = 'contacts'::regclass AND array_length(c.conkey, 1) = 1 "
                        + "ORDER BY 1");
        if (removeHasContact) {
            for (Map<String, Object> fk : fks) {
                String tbl = (String) fk.get("tbl");
                String col = (String) fk.get("col");
                moveRefs(tbl, col, keepId, removeId, moved, skipped);
            }
        }
        // Filhas e papéis da pessoa.
        jdbc.update("UPDATE person_contacts SET person_id = ?, sync_slot = NULL, is_primary = FALSE WHERE person_id = ? AND sync_slot IS NULL",
                keepId, removeId);
        jdbc.update("UPDATE person_addresses SET person_id = ?, is_primary = FALSE WHERE person_id = ? AND sync_slot IS NULL", keepId, removeId);
        jdbc.update("UPDATE person_documents SET person_id = ?, is_primary = FALSE WHERE person_id = ?", keepId, removeId);
        jdbc.update("UPDATE person_bank_accounts SET person_id = ?, is_primary = FALSE WHERE person_id = ?", keepId, removeId);
        moveRefs("person_relationships", "person_id", keepId, removeId, moved, skipped);
        moveRefs("person_relationships", "related_person_id", keepId, removeId, moved, skipped);
        jdbc.update("DELETE FROM person_relationships WHERE person_id = related_person_id");
        for (String role : List.of("suppliers", "employees")) {
            Boolean keepHas = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM " + role + " WHERE person_id = ?)", Boolean.class, keepId);
            if (!Boolean.TRUE.equals(keepHas)) moveRefs(role, "person_id", keepId, removeId, moved, skipped);
        }
        Boolean keepCustomer = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM customers WHERE person_id = ?)", Boolean.class, keepId);
        Boolean removeCustomer = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM customers WHERE person_id = ?)", Boolean.class, removeId);
        if (!Boolean.TRUE.equals(keepCustomer) && Boolean.TRUE.equals(removeCustomer)) {
            jdbc.update("INSERT INTO customers (person_id, company_id, customer_code, customer_since) "
                    + "SELECT ?, company_id, NULL, customer_since FROM customers WHERE person_id = ?", keepId, removeId);
        }

        // Remove o duplicado (lógico) e fecha as pendências.
        if (removeHasContact) {
            jdbc.update("UPDATE contacts SET deleted_at = now(), is_patient = FALSE, updated_at = now() WHERE id = ?", removeId);
        }
        jdbc.update("UPDATE persons SET deleted = TRUE, deleted_at = coalesce(deleted_at, now()), deleted_by = ?, delete_reason = ?, "
                + "active = FALSE WHERE id = ?", user.userId(), "Unificado com outro cadastro: " + reason.trim(), removeId);
        jdbc.update("UPDATE person_migration_issues SET resolved_at = now(), resolved_by = ? WHERE company_id = ? "
                + "AND person_id IN (?, ?) AND resolved_at IS NULL", user.userId(), companyId, keepId, removeId);
        history.record(companyId, keepId, user.userId(), "MERGE", "persons", null,
                Map.of("mergedPersonId", removeId.toString(), "movedTables", String.join(",", moved)), reason.trim());
        history.record(companyId, removeId, user.userId(), "MERGE", "persons", null,
                Map.of("keptPersonId", keepId.toString()), reason.trim());
        return new MergeResult(keepId, removeId, moved, skipped);
    }

    /** UPDATE dentro de um savepoint: conflito de unicidade não derruba a unificação inteira. */
    private void moveRefs(String table, String column, UUID keepId, UUID removeId, List<String> moved, List<String> skipped) {
        if (!table.matches("[a-z_.\"]+") || !column.matches("[a-z_]+")) return;
        jdbc.execute("SAVEPOINT merge_step");
        try {
            int n = jdbc.update("UPDATE " + table + " SET " + column + " = ? WHERE " + column + " = ?", keepId, removeId);
            jdbc.execute("RELEASE SAVEPOINT merge_step");
            if (n > 0) moved.add(table + "(" + n + ")");
        } catch (DataAccessException e) {
            jdbc.execute("ROLLBACK TO SAVEPOINT merge_step");
            skipped.add(table);
        }
    }
}
