package com.becommerce.crm.masterdata.person;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ponte com contacts durante a transição: cria a linha de CRM (mesmo id) para uma pessoa que
 * vai virar cliente. Os contatos e o endereço já cadastrados viram os "espelhos" (sync_slot)
 * antes do INSERT, para o gatilho app.sync_person_from_contact não duplicá-los.
 */
@Component
public class PersonContactBridge {

    private final JdbcTemplate jdbc;

    public PersonContactBridge(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean hasContact(UUID companyId, UUID personId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM contacts WHERE company_id = ? AND id = ?)",
                Boolean.class, companyId, personId));
    }

    public void createContactFor(UUID companyId, UUID personId) {
        if (hasContact(companyId, personId)) return;
        Map<String, Object> p = jdbc.queryForMap("SELECT * FROM persons WHERE company_id = ? AND id = ?", companyId, personId);
        UUID phoneId = firstChannel(personId, "MOBILE");
        if (phoneId == null) phoneId = firstChannel(personId, "PHONE");
        UUID emailId = firstChannel(personId, "EMAIL");
        String phone = phoneId == null ? null
                : jdbc.queryForObject("SELECT value FROM person_contacts WHERE id = ?", String.class, phoneId);
        String email = emailId == null ? null
                : jdbc.queryForObject("SELECT value FROM person_contacts WHERE id = ?", String.class, emailId);
        if (email != null && Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM contacts WHERE company_id = ? "
                + "AND lower(email) = lower(?) AND deleted_at IS NULL)", Boolean.class, companyId, email))) {
            email = null;
            emailId = null;
        }
        if (phoneId != null) jdbc.update("UPDATE person_contacts SET sync_slot = 'CONTACT_PHONE' WHERE id = ?", phoneId);
        if (emailId != null) jdbc.update("UPDATE person_contacts SET sync_slot = 'CONTACT_EMAIL' WHERE id = ?", emailId);
        List<UUID> addr = jdbc.queryForList("SELECT id FROM person_addresses WHERE person_id = ? AND active "
                + "ORDER BY is_primary DESC, created_at LIMIT 1", UUID.class, personId);
        Map<String, Object> a = Map.of();
        if (!addr.isEmpty()) {
            jdbc.update("UPDATE person_addresses SET sync_slot = 'CONTACT_ADDRESS' WHERE id = ?", addr.get(0));
            a = jdbc.queryForMap("SELECT * FROM person_addresses WHERE id = ?", addr.get(0));
        }
        String[] parts = PersonService.splitName((String) p.get("name"));
        jdbc.update("INSERT INTO contacts (id, company_id, first_name, last_name, email, phone, cpf, rg, rg_issuer, birth_date, "
                        + "gender, marital_status, professional_status, notes, zip_code, street, address_number, "
                        + "address_complement, neighborhood, city, state, deleted_at) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                personId, companyId, parts[0], parts[1], email, PersonService.trunc(phone, 20), p.get("tax_id"),
                PersonService.trunc((String) p.get("rg_or_ie"), 20), p.get("rg_issuer"), p.get("birth_or_foundation_date"),
                p.get("gender"), p.get("marital_status"), p.get("professional_status"), p.get("notes"),
                a.get("postal_code"), a.get("street"), a.get("address_number"), a.get("complement"),
                a.get("neighborhood_name"), a.get("city_name"), a.get("uf"), p.get("deleted_at"));
        // O gatilho recalcula a cidade pelo texto; mantém a escolhida.
        if (a.get("city_id") != null) {
            jdbc.update("UPDATE person_addresses SET city_id = ? WHERE id = ?", a.get("city_id"), addr.get(0));
        }
    }

    private UUID firstChannel(UUID personId, String type) {
        List<UUID> l = jdbc.queryForList("SELECT id FROM person_contacts WHERE person_id = ? AND contact_type = ? AND active "
                + "ORDER BY is_primary DESC, created_at LIMIT 1", UUID.class, personId, type);
        return l.isEmpty() ? null : l.get(0);
    }
}
