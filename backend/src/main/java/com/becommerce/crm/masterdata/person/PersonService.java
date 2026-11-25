package com.becommerce.crm.masterdata.person;

import com.becommerce.crm.masterdata.company.application.service.CompanyModulesService;
import com.becommerce.crm.masterdata.company.domain.CompanySegment;
import com.becommerce.crm.masterdata.person.PersonDtos.*;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Cadastro central de pessoas (doc seções 4, 9 e 22.2).
 *
 * Transição (até a etapa 8): quando a pessoa tem linha em contacts (mesmo id), os dados de
 * identidade, o telefone/celular/e-mail e o endereço "espelhados" (sync_slot) são gravados em
 * contacts e o gatilho app.sync_person_from_contact (V147) atualiza persons. Assim agenda,
 * prontuário, Inbox e as telas antigas continuam vendo o mesmo dado. Sem contacts (fornecedor,
 * funcionário), grava direto em persons.
 */
@Service
public class PersonService {

    static final Set<String> PERSON_TYPES = Set.of("F", "J");
    static final Set<String> ADDRESS_TYPES = Set.of("RESIDENTIAL", "COMMERCIAL", "BILLING", "DELIVERY", "OTHER");
    static final Set<String> CONTACT_TYPES = Set.of("MOBILE", "PHONE", "EMAIL", "WEBSITE", "OTHER");
    static final Set<String> DOCUMENT_TYPES = Set.of("RG", "CNH", "PASSPORT", "CNS", "RNE", "SOCIAL_CONTRACT", "OTHER");
    static final Set<String> RELATIONSHIP_TYPES = Set.of("GUARDIAN", "SPOUSE", "PARTNER", "LEGAL_REPRESENTATIVE", "OTHER");
    static final Set<String> ACCOUNT_TYPES = Set.of("CHECKING", "SAVINGS", "PAYMENT", "OTHER");
    static final Set<String> PIX_TYPES = Set.of("CPF", "CNPJ", "EMAIL", "PHONE", "RANDOM");

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;
    private final PersonHistoryService history;
    private final PersonFieldSettingsService fields;
    private final CompanyModulesService modules;
    private final BankDataCrypto crypto;
    private final PersonRoleService roles;

    public PersonService(JdbcTemplate jdbc, NamedParameterJdbcTemplate named, PersonHistoryService history,
                         PersonFieldSettingsService fields, CompanyModulesService modules, BankDataCrypto crypto,
                         PersonRoleService roles) {
        this.jdbc = jdbc;
        this.named = named;
        this.history = history;
        this.fields = fields;
        this.modules = modules;
        this.crypto = crypto;
        this.roles = roles;
    }

    // =================================================================== consulta

    @Transactional(readOnly = true)
    public PageResult<PersonSummary> search(UUID companyId, String q, String type, String role, Boolean active,
                                            Boolean provisional, String uf, int page, int size, CurrentUser user) {
        MapSqlParameterSource ps = new MapSqlParameterSource("c", companyId);
        StringBuilder where = new StringBuilder(" WHERE p.company_id = :c AND NOT p.deleted");
        appendSearch(where, ps, q);
        if (type != null && PERSON_TYPES.contains(type)) {
            where.append(" AND p.person_type = :t");
            ps.addValue("t", type);
        }
        if (role != null) {
            switch (role) {
                case "CUSTOMER" -> where.append(" AND EXISTS (SELECT 1 FROM customers x WHERE x.person_id = p.id)");
                case "PATIENT" -> where.append(" AND EXISTS (SELECT 1 FROM patients x WHERE x.person_id = p.id)");
                case "SUPPLIER" -> where.append(" AND EXISTS (SELECT 1 FROM suppliers x WHERE x.person_id = p.id)");
                case "EMPLOYEE" -> where.append(" AND EXISTS (SELECT 1 FROM employees x WHERE x.person_id = p.id)");
                case "NONE" -> where.append(" AND NOT EXISTS (SELECT 1 FROM customers x WHERE x.person_id = p.id)"
                        + " AND NOT EXISTS (SELECT 1 FROM suppliers x WHERE x.person_id = p.id)"
                        + " AND NOT EXISTS (SELECT 1 FROM employees x WHERE x.person_id = p.id)");
                default -> { }
            }
        }
        if (active != null) {
            where.append(" AND p.active = :a");
            ps.addValue("a", active);
        }
        if (provisional != null) {
            where.append(" AND p.provisional = :pv");
            ps.addValue("pv", provisional);
        }
        if (uf != null && !uf.isBlank()) {
            where.append(" AND EXISTS (SELECT 1 FROM person_addresses a WHERE a.person_id = p.id AND a.uf = :uf)");
            ps.addValue("uf", uf.trim().toUpperCase(Locale.ROOT));
        }
        long total = Optional.ofNullable(named.queryForObject("SELECT count(*) FROM persons p" + where, ps, Long.class)).orElse(0L);
        int sz = Math.max(1, Math.min(size, 200));
        int pg = Math.max(0, page);
        ps.addValue("lim", sz).addValue("off", pg * sz);
        boolean sensitive = PersonAccess.has(user, "person:sensitive");
        List<PersonSummary> items = named.query("SELECT p.id, p.person_type, p.name, p.trade_name, p.tax_id, p.provisional, "
                + "p.active, p.created_at, "
                + "EXISTS (SELECT 1 FROM customers x WHERE x.person_id = p.id) AS is_customer, "
                + "EXISTS (SELECT 1 FROM patients x WHERE x.person_id = p.id) AS is_patient, "
                + "EXISTS (SELECT 1 FROM suppliers x WHERE x.person_id = p.id) AS is_supplier, "
                + "EXISTS (SELECT 1 FROM employees x WHERE x.person_id = p.id) AS is_employee, "
                + "(SELECT value FROM person_contacts pc WHERE pc.person_id = p.id AND pc.active AND pc.contact_type = 'MOBILE' "
                + "  ORDER BY pc.is_primary DESC, pc.created_at LIMIT 1) AS mobile, "
                + "(SELECT value FROM person_contacts pc WHERE pc.person_id = p.id AND pc.active AND pc.contact_type = 'EMAIL' "
                + "  ORDER BY pc.is_primary DESC, pc.created_at LIMIT 1) AS email, "
                + "(SELECT city_name FROM person_addresses a WHERE a.person_id = p.id AND a.active "
                + "  ORDER BY a.is_primary DESC, a.created_at LIMIT 1) AS city, "
                + "(SELECT uf FROM person_addresses a WHERE a.person_id = p.id AND a.active "
                + "  ORDER BY a.is_primary DESC, a.created_at LIMIT 1) AS uf "
                + "FROM persons p" + where + " ORDER BY lower(p.name), p.id LIMIT :lim OFFSET :off", ps,
                (rs, i) -> {
                    List<String> r = new ArrayList<>();
                    if (rs.getBoolean("is_customer")) r.add("CUSTOMER");
                    if (rs.getBoolean("is_patient")) r.add("PATIENT");
                    if (rs.getBoolean("is_supplier")) r.add("SUPPLIER");
                    if (rs.getBoolean("is_employee")) r.add("EMPLOYEE");
                    return new PersonSummary(id(rs, "id"), rs.getString("person_type"), rs.getString("name"),
                            rs.getString("trade_name"), showTax(rs.getString("tax_id"), sensitive),
                            rs.getBoolean("provisional"), rs.getBoolean("active"), r, rs.getString("mobile"),
                            rs.getString("email"), rs.getString("city"), rs.getString("uf"),
                            rs.getObject("created_at", OffsetDateTime.class));
                });
        return new PageResult<>(items, total, pg, sz);
    }

    /** Filtro de texto comum: nome/nome fantasia, CPF/CNPJ ou valor de contato. */
    static void appendSearch(StringBuilder where, MapSqlParameterSource ps, String q) {
        if (q == null || q.isBlank()) return;
        String term = q.trim().toLowerCase(Locale.ROOT);
        String digits = TaxIdValidator.normalize(q);
        where.append(" AND (lower(p.name) LIKE :q OR lower(coalesce(p.trade_name, '')) LIKE :q"
                + " OR EXISTS (SELECT 1 FROM person_contacts qc WHERE qc.person_id = p.id AND lower(qc.value) LIKE :q)");
        ps.addValue("q", "%" + term + "%");
        if (digits != null && digits.length() >= 3) {
            where.append(" OR p.tax_id LIKE :qd OR EXISTS (SELECT 1 FROM person_contacts qd WHERE qd.person_id = p.id"
                    + " AND qd.value LIKE :qd)");
            ps.addValue("qd", "%" + digits + "%");
        }
        where.append(")");
    }

    @Transactional(readOnly = true)
    public PersonDetail get(UUID companyId, UUID personId, CurrentUser user) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM persons WHERE company_id = ? AND id = ?",
                companyId, personId);
        if (rows.isEmpty()) throw PersonException.notFound("Cadastro");
        Map<String, Object> p = rows.get(0);
        boolean sensitive = PersonAccess.has(user, "person:sensitive");
        boolean health = isHealth(companyId);
        boolean hasContact = hasContact(companyId, personId);
        List<String> r = roles.rolesOf(companyId, personId);
        long issues = Optional.ofNullable(jdbc.queryForObject("SELECT count(*) FROM person_migration_issues "
                + "WHERE company_id = ? AND person_id = ? AND resolved_at IS NULL", Long.class, companyId, personId)).orElse(0L);
        return new PersonDetail(personId, (String) p.get("person_type"), (String) p.get("name"),
                (String) p.get("trade_name"), showTax((String) p.get("tax_id"), sensitive), (String) p.get("rg_or_ie"),
                (String) p.get("rg_issuer"), date(p.get("birth_or_foundation_date")), (String) p.get("gender"),
                (String) p.get("marital_status"), (String) p.get("professional_status"), (String) p.get("nationality"),
                (Integer) p.get("country_id"), bool(p.get("provisional")), bool(p.get("active")), bool(p.get("deleted")),
                (String) p.get("delete_reason"), (String) p.get("notes"), (String) p.get("avatar_url"), hasContact,
                health, r, addresses(companyId, personId), channels(companyId, personId),
                sensitive ? documents(companyId, personId) : List.of(), relationships(companyId, personId),
                roles.customer(companyId, personId), roles.supplier(companyId, personId),
                roles.employee(companyId, personId, PersonAccess.has(user, "employee:salary")), issues,
                odt(p.get("created_at")), odt(p.get("updated_at")));
    }

    /** Aviso de duplicidade durante a digitação (CPF/CNPJ ou telefone). */
    @Transactional(readOnly = true)
    public List<DuplicateMember> check(UUID companyId, String taxId, String phone, UUID excludeId) {
        String tax = TaxIdValidator.normalize(taxId);
        String ph = phone == null ? null : phone.replaceAll("\\D", "");
        if ((tax == null || tax.length() < 11) && (ph == null || ph.length() < 8)) return List.of();
        MapSqlParameterSource ps = new MapSqlParameterSource("c", companyId).addValue("x", excludeId)
                .addValue("t", tax).addValue("ph", ph == null || ph.length() < 8 ? null : "%" + ph.substring(Math.max(0, ph.length() - 8)));
        return named.query("SELECT p.id, p.name, p.tax_id, p.birth_or_foundation_date, p.created_at, "
                + "(SELECT value FROM person_contacts pc WHERE pc.person_id = p.id AND pc.contact_type = 'MOBILE' "
                + " ORDER BY pc.is_primary DESC LIMIT 1) AS mobile "
                + "FROM persons p WHERE p.company_id = :c AND NOT p.deleted AND (CAST(:x AS uuid) IS NULL OR p.id <> :x) AND ("
                + "(CAST(:t AS text) IS NOT NULL AND p.tax_id = :t) OR (CAST(:ph AS text) IS NOT NULL AND EXISTS ("
                + "SELECT 1 FROM person_contacts pc WHERE pc.person_id = p.id AND pc.value LIKE :ph))) LIMIT 5", ps,
                (rs, i) -> new DuplicateMember(id(rs, "id"), rs.getString("name"), TaxIdValidator.mask(rs.getString("tax_id")),
                        rs.getObject("birth_or_foundation_date", LocalDate.class), rs.getString("mobile"),
                        rs.getObject("created_at", OffsetDateTime.class), null, null));
    }

    @Transactional(readOnly = true)
    public List<HistoryEntry> history(UUID companyId, UUID personId) {
        requirePerson(companyId, personId);
        return history.list(companyId, personId);
    }

    // =================================================================== gravação

    @Transactional
    public PersonDetail create(UUID companyId, PersonRequest req, CurrentUser user) {
        UUID userId = user.userId();
        Normalized n = normalize(req, null);
        ensureUniqueTax(companyId, n.taxId, null);
        UUID id = UUID.randomUUID();
        Set<String> wanted = new LinkedHashSet<>(req.roles() == null ? List.of() : req.roles());
        boolean needsContact = wanted.contains("CUSTOMER");
        if (needsContact) {
            insertContact(companyId, id, n, req.rgIssuer(), req.mobile(), req.email(), req.address());
        } else {
            jdbc.update("INSERT INTO persons (id, company_id, person_type, name, trade_name, tax_id, rg_or_ie, rg_issuer, "
                            + "birth_or_foundation_date, gender, marital_status, professional_status, nationality, country_id, "
                            + "provisional, notes, created_by, updated_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    id, companyId, n.type, n.name, n.tradeName, n.taxId, n.rgOrIe, req.rgIssuer(),
                    req.birthOrFoundationDate(), blank(req.gender()), blank(req.maritalStatus()),
                    blank(req.professionalStatus()), blank(req.nationality()), req.countryId(), n.provisional,
                    blank(req.notes()), userId, userId);
            if (notBlank(req.mobile())) addChannel(companyId, id, new ContactChannelRequest("MOBILE", req.mobile(), null, true, true), userId, false);
            if (notBlank(req.email())) addChannel(companyId, id, new ContactChannelRequest("EMAIL", req.email(), null, false, true), userId, false);
            if (req.address() != null && hasAddressData(req.address())) addAddress(companyId, id, req.address(), userId, false);
        }
        jdbc.update("UPDATE persons SET person_type = ?, trade_name = ?, nationality = ?, country_id = ?, provisional = ?, "
                        + "created_by = ?, updated_by = ? WHERE company_id = ? AND id = ?",
                n.type, n.tradeName, blank(req.nationality()), req.countryId(), n.provisional, userId, userId, companyId, id);
        history.record(companyId, id, userId, "CREATE", "persons", null, snapshot(companyId, id), null);

        List<String> ctx = new ArrayList<>();
        if (wanted.contains("CUSTOMER")) {
            roles.enableCustomer(companyId, id, req.customer(), user, false);
            ctx.add("CUSTOMER");
        }
        if (wanted.contains("SUPPLIER")) {
            roles.enableSupplier(companyId, id, req.supplier(), user, false);
            ctx.add("SUPPLIER");
        }
        if (wanted.contains("EMPLOYEE")) {
            roles.enableEmployee(companyId, id, req.employee(), user, false);
            ctx.add("EMPLOYEE");
        }
        fields.validate(companyId, id, ctx);
        return get(companyId, id, user);
    }

    @Transactional
    public PersonDetail update(UUID companyId, UUID personId, PersonRequest req, CurrentUser user) {
        UUID userId = user.userId();
        Map<String, Object> before = snapshot(companyId, personId);
        boolean sensitive = PersonAccess.has(user, "person:sensitive");
        // Sem permissão, o CPF chega mascarado: mantém o gravado.
        String keepTax = (String) before.get("taxId");
        PersonRequest effective = req;
        if (!sensitive || (req.taxId() != null && req.taxId().contains("*"))) {
            effective = withTax(req, keepTax);
        }
        Normalized n = normalize(effective, keepTax);
        ensureUniqueTax(companyId, n.taxId, personId);
        if (hasContact(companyId, personId)) {
            String[] parts = splitName(n.name);
            try {
                jdbc.update("UPDATE contacts SET first_name = ?, last_name = ?, cpf = ?, rg = ?, rg_issuer = ?, birth_date = ?, "
                                + "gender = ?, marital_status = ?, professional_status = ?, notes = ?, updated_at = now() "
                                + "WHERE company_id = ? AND id = ?",
                        parts[0], parts[1], n.taxId, trunc(n.rgOrIe, 20), trunc(effective.rgIssuer(), 20),
                        effective.birthOrFoundationDate(), blank(effective.gender()), blank(effective.maritalStatus()),
                        blank(effective.professionalStatus()), blank(effective.notes()), companyId, personId);
            } catch (DataIntegrityViolationException e) {
                throw PersonException.badRequest("Não foi possível salvar: confira os dados informados.");
            }
            jdbc.update("UPDATE persons SET person_type = ?, trade_name = ?, rg_or_ie = ?, nationality = ?, country_id = ?, "
                            + "provisional = ?, updated_by = ?, updated_at = now() WHERE company_id = ? AND id = ?",
                    n.type, n.tradeName, n.rgOrIe, blank(effective.nationality()), effective.countryId(), n.provisional,
                    userId, companyId, personId);
        } else {
            jdbc.update("UPDATE persons SET person_type = ?, name = ?, trade_name = ?, tax_id = ?, rg_or_ie = ?, rg_issuer = ?, "
                            + "birth_or_foundation_date = ?, gender = ?, marital_status = ?, professional_status = ?, "
                            + "nationality = ?, country_id = ?, provisional = ?, notes = ?, updated_by = ?, updated_at = now() "
                            + "WHERE company_id = ? AND id = ?",
                    n.type, n.name, n.tradeName, n.taxId, n.rgOrIe, blank(effective.rgIssuer()),
                    effective.birthOrFoundationDate(), blank(effective.gender()), blank(effective.maritalStatus()),
                    blank(effective.professionalStatus()), blank(effective.nationality()), effective.countryId(),
                    n.provisional, blank(effective.notes()), userId, companyId, personId);
        }
        if (n.taxId != null) {
            jdbc.update("UPDATE person_migration_issues SET resolved_at = now(), resolved_by = ? WHERE company_id = ? "
                    + "AND person_id = ? AND resolved_at IS NULL AND issue_type IN ('INVALID_TAX_ID','DUPLICATE_OTHER_PERSON')",
                    userId, companyId, personId);
        }
        history.record(companyId, personId, userId, "UPDATE", "persons", before, snapshot(companyId, personId), null);
        fields.validate(companyId, personId, roles.roleContexts(companyId, personId));
        return get(companyId, personId, user);
    }

    @Transactional
    public PersonDetail setStatus(UUID companyId, UUID personId, boolean active, CurrentUser user) {
        requirePerson(companyId, personId);
        jdbc.update("UPDATE persons SET active = ?, updated_by = ?, updated_at = now() WHERE company_id = ? AND id = ?",
                active, user.userId(), companyId, personId);
        history.record(companyId, personId, user.userId(), "UPDATE", "persons", Map.of("active", !active),
                Map.of("active", active), null);
        return get(companyId, personId, user);
    }

    @Transactional
    public void softDelete(UUID companyId, UUID personId, String reason, CurrentUser user) {
        requirePerson(companyId, personId);
        if (reason == null || reason.isBlank()) throw PersonException.badRequest("Informe o motivo da exclusão.");
        if (hasContact(companyId, personId)) {
            jdbc.update("UPDATE contacts SET deleted_at = now(), updated_at = now() WHERE company_id = ? AND id = ? "
                    + "AND deleted_at IS NULL", companyId, personId);
        }
        jdbc.update("UPDATE persons SET deleted = TRUE, deleted_at = coalesce(deleted_at, now()), deleted_by = ?, "
                + "delete_reason = ?, active = FALSE, updated_at = now() WHERE company_id = ? AND id = ?",
                user.userId(), reason.trim(), companyId, personId);
        history.record(companyId, personId, user.userId(), "SOFT_DELETE", "persons", null, null, reason.trim());
    }

    @Transactional
    public PersonDetail restore(UUID companyId, UUID personId, CurrentUser user) {
        Map<String, Object> p = jdbc.queryForMap("SELECT tax_id FROM persons WHERE company_id = ? AND id = ?", companyId, personId);
        ensureUniqueTax(companyId, (String) p.get("tax_id"), personId);
        if (hasContact(companyId, personId)) {
            jdbc.update("UPDATE contacts SET deleted_at = NULL, updated_at = now() WHERE company_id = ? AND id = ?", companyId, personId);
        }
        jdbc.update("UPDATE persons SET deleted = FALSE, deleted_at = NULL, deleted_by = NULL, delete_reason = NULL, "
                + "active = TRUE, updated_at = now() WHERE company_id = ? AND id = ?", companyId, personId);
        history.record(companyId, personId, user.userId(), "RESTORE", "persons", null, null, null);
        return get(companyId, personId, user);
    }

    // =================================================================== endereços

    @Transactional
    public List<Address> addAddress(UUID companyId, UUID personId, AddressRequest req, UUID userId, boolean record) {
        requirePerson(companyId, personId);
        AddressValues v = addressValues(req);
        boolean primary = req.primary() == null
                ? !Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM person_addresses WHERE person_id = ? "
                + "AND address_type = ? AND is_primary AND active)", Boolean.class, personId, v.type))
                : req.primary();
        if (primary) clearPrimary("person_addresses", "address_type", personId, v.type, null);
        // Pessoa com contacts e sem endereço espelhado: este passa a ser o espelho (telas antigas veem).
        boolean mirror = hasContact(companyId, personId) && !Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM person_addresses WHERE person_id = ? AND sync_slot = 'CONTACT_ADDRESS')",
                Boolean.class, personId));
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO person_addresses (id, company_id, person_id, address_type, postal_code, street, address_number, "
                        + "complement, neighborhood_id, neighborhood_name, city_id, city_name, uf, country_id, foreign_region, "
                        + "is_primary, sync_slot) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                id, companyId, personId, v.type, v.postal, v.street, v.number, v.complement, req.neighborhoodId(),
                v.neighborhood, req.cityId(), v.city, v.uf, req.countryId(), blank(req.foreignRegion()), primary,
                mirror ? "CONTACT_ADDRESS" : null);
        if (mirror) writeContactAddress(companyId, personId, id);
        if (record) history.record(companyId, personId, userId, "UPDATE", "person_addresses", null, Map.of("address", v.summary()), null);
        touch(companyId, personId, userId);
        return addresses(companyId, personId);
    }

    @Transactional
    public List<Address> updateAddress(UUID companyId, UUID personId, UUID addressId, AddressRequest req, UUID userId) {
        Map<String, Object> row = child("person_addresses", companyId, personId, addressId);
        AddressValues v = addressValues(req);
        boolean primary = req.primary() == null ? bool(row.get("is_primary")) : req.primary();
        if (primary) clearPrimary("person_addresses", "address_type", personId, v.type, addressId);
        jdbc.update("UPDATE person_addresses SET address_type = ?, postal_code = ?, street = ?, address_number = ?, complement = ?, "
                        + "neighborhood_id = ?, neighborhood_name = ?, city_id = ?, city_name = ?, uf = ?, country_id = ?, "
                        + "foreign_region = ?, is_primary = ?, updated_at = now() WHERE id = ?",
                v.type, v.postal, v.street, v.number, v.complement, req.neighborhoodId(), v.neighborhood, req.cityId(),
                v.city, v.uf, req.countryId(), blank(req.foreignRegion()), primary, addressId);
        if ("CONTACT_ADDRESS".equals(row.get("sync_slot"))) writeContactAddress(companyId, personId, addressId);
        history.record(companyId, personId, userId, "UPDATE", "person_addresses",
                Map.of("address", summary(row)), Map.of("address", v.summary()), null);
        touch(companyId, personId, userId);
        return addresses(companyId, personId);
    }

    @Transactional
    public List<Address> deleteAddress(UUID companyId, UUID personId, UUID addressId, UUID userId) {
        Map<String, Object> row = child("person_addresses", companyId, personId, addressId);
        if ("CONTACT_ADDRESS".equals(row.get("sync_slot")) && hasContact(companyId, personId)) {
            jdbc.update("UPDATE contacts SET zip_code = NULL, street = NULL, address_number = NULL, address_complement = NULL, "
                    + "neighborhood = NULL, city = NULL, state = NULL, updated_at = now() WHERE company_id = ? AND id = ?",
                    companyId, personId);
        }
        jdbc.update("DELETE FROM person_addresses WHERE id = ?", addressId);
        history.record(companyId, personId, userId, "UPDATE", "person_addresses", Map.of("address", summary(row)), null, null);
        touch(companyId, personId, userId);
        return addresses(companyId, personId);
    }

    /** Copia o endereço espelhado para as colunas de contacts (o gatilho mantém a linha igual). */
    private void writeContactAddress(UUID companyId, UUID personId, UUID addressId) {
        Map<String, Object> a = jdbc.queryForMap("SELECT postal_code, street, address_number, complement, neighborhood_name, "
                + "city_name, uf, city_id FROM person_addresses WHERE id = ?", addressId);
        jdbc.update("UPDATE contacts SET zip_code = ?, street = ?, address_number = ?, address_complement = ?, neighborhood = ?, "
                        + "city = ?, state = ?, updated_at = now() WHERE company_id = ? AND id = ?",
                a.get("postal_code"), trunc((String) a.get("street"), 160), trunc((String) a.get("address_number"), 20),
                trunc((String) a.get("complement"), 80), trunc((String) a.get("neighborhood_name"), 80),
                trunc((String) a.get("city_name"), 80), a.get("uf"), companyId, personId);
        // O gatilho de contacts pode ter recalculado a cidade pelo texto: mantém a escolhida na tela.
        if (a.get("city_id") != null) {
            jdbc.update("UPDATE person_addresses SET city_id = ? WHERE id = ? AND city_id IS DISTINCT FROM ?",
                    a.get("city_id"), addressId, a.get("city_id"));
        }
    }

    // =================================================================== contatos

    @Transactional
    public List<ContactChannel> addChannel(UUID companyId, UUID personId, ContactChannelRequest req, UUID userId, boolean record) {
        requirePerson(companyId, personId);
        String type = upper(req.contactType());
        if (!CONTACT_TYPES.contains(type)) throw PersonException.badRequest("Tipo de contato inválido.");
        String value = channelValue(type, req.value());
        boolean primary = req.primary() == null
                ? !Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM person_contacts WHERE person_id = ? "
                + "AND contact_type = ? AND is_primary AND active)", Boolean.class, personId, type))
                : req.primary();
        if (primary) clearPrimary("person_contacts", "contact_type", personId, type, null);
        String slot = null;
        if (hasContact(companyId, personId)) {
            if ("EMAIL".equals(type) && !slotExists(personId, "CONTACT_EMAIL")) slot = "CONTACT_EMAIL";
            else if (("MOBILE".equals(type) || "PHONE".equals(type)) && !slotExists(personId, "CONTACT_PHONE")) slot = "CONTACT_PHONE";
            else if ("MOBILE".equals(type) && !slotExists(personId, "CONTACT_MOBILE")) slot = "CONTACT_MOBILE";
        }
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO person_contacts (id, company_id, person_id, contact_type, value, contact_name, is_whatsapp, "
                        + "is_primary, sync_slot) VALUES (?,?,?,?,?,?,?,?,?)",
                id, companyId, personId, type, value, blank(req.contactName()), Boolean.TRUE.equals(req.whatsapp()),
                primary, slot);
        if (slot != null) writeContactChannel(companyId, personId, slot, value);
        if (record) history.record(companyId, personId, userId, "UPDATE", "person_contacts", null, Map.of(type, value), null);
        touch(companyId, personId, userId);
        return channels(companyId, personId);
    }

    @Transactional
    public List<ContactChannel> updateChannel(UUID companyId, UUID personId, UUID channelId, ContactChannelRequest req, UUID userId) {
        Map<String, Object> row = child("person_contacts", companyId, personId, channelId);
        String type = req.contactType() == null ? (String) row.get("contact_type") : upper(req.contactType());
        if (!CONTACT_TYPES.contains(type)) throw PersonException.badRequest("Tipo de contato inválido.");
        String value = channelValue(type, req.value());
        boolean primary = req.primary() == null ? bool(row.get("is_primary")) : req.primary();
        if (primary) clearPrimary("person_contacts", "contact_type", personId, type, channelId);
        String slot = (String) row.get("sync_slot");
        if ("CONTACT_EMAIL".equals(slot) && !"EMAIL".equals(type)) slot = null;
        if (slot != null && slot.startsWith("CONTACT_") && !"CONTACT_EMAIL".equals(slot) && "EMAIL".equals(type)) slot = null;
        if (slot == null && row.get("sync_slot") != null && hasContact(companyId, personId)) {
            writeContactChannel(companyId, personId, (String) row.get("sync_slot"), null);
        }
        jdbc.update("UPDATE person_contacts SET contact_type = ?, value = ?, contact_name = ?, is_whatsapp = ?, is_primary = ?, "
                        + "sync_slot = ?, updated_at = now() WHERE id = ?",
                type, value, blank(req.contactName()), req.whatsapp() == null ? bool(row.get("is_whatsapp")) : req.whatsapp(),
                primary, slot, channelId);
        if (slot != null && hasContact(companyId, personId)) writeContactChannel(companyId, personId, slot, value);
        history.record(companyId, personId, userId, "UPDATE", "person_contacts",
                Map.of(row.get("contact_type").toString(), row.get("value")), Map.of(type, value), null);
        touch(companyId, personId, userId);
        return channels(companyId, personId);
    }

    @Transactional
    public List<ContactChannel> deleteChannel(UUID companyId, UUID personId, UUID channelId, UUID userId) {
        Map<String, Object> row = child("person_contacts", companyId, personId, channelId);
        String slot = (String) row.get("sync_slot");
        jdbc.update("DELETE FROM person_contacts WHERE id = ?", channelId);
        if (slot != null && hasContact(companyId, personId)) writeContactChannel(companyId, personId, slot, null);
        history.record(companyId, personId, userId, "UPDATE", "person_contacts",
                Map.of(row.get("contact_type").toString(), row.get("value")), null, null);
        touch(companyId, personId, userId);
        return channels(companyId, personId);
    }

    private void writeContactChannel(UUID companyId, UUID personId, String slot, String value) {
        String column = switch (slot) {
            case "CONTACT_PHONE" -> "phone";
            case "CONTACT_MOBILE" -> "mobile";
            case "CONTACT_EMAIL" -> "email";
            default -> null;
        };
        if (column == null) return;
        try {
            jdbc.update("UPDATE contacts SET " + column + " = ?, updated_at = now() WHERE company_id = ? AND id = ?",
                    "email".equals(column) ? value : trunc(value, 20), companyId, personId);
        } catch (DuplicateKeyException e) {
            throw PersonException.conflict("EMAIL_IN_USE", "Este e-mail já está em outro cadastro.");
        }
    }

    // =================================================================== documentos

    @Transactional
    public List<Document> addDocument(UUID companyId, UUID personId, DocumentRequest req, UUID userId) {
        requirePerson(companyId, personId);
        String type = upper(req.documentType());
        if (!DOCUMENT_TYPES.contains(type)) throw PersonException.badRequest("Tipo de documento inválido.");
        if (req.documentNumber() == null || req.documentNumber().isBlank()) throw PersonException.badRequest("Informe o número do documento.");
        jdbc.update("INSERT INTO person_documents (company_id, person_id, document_type, document_number, issuer, issuer_uf, "
                        + "issue_date, expiry_date, storage_object_id, is_primary) VALUES (?,?,?,?,?,?,?,?,?,?)",
                companyId, personId, type, req.documentNumber().trim(), blank(req.issuer()), upperBlank(req.issuerUf()),
                req.issueDate(), req.expiryDate(), req.storageObjectId(), Boolean.TRUE.equals(req.primary()));
        history.record(companyId, personId, userId, "UPDATE", "person_documents", null,
                Map.of("documentType", type, "documentNumber", req.documentNumber()), null);
        touch(companyId, personId, userId);
        return documents(companyId, personId);
    }

    @Transactional
    public List<Document> updateDocument(UUID companyId, UUID personId, UUID docId, DocumentRequest req, UUID userId) {
        Map<String, Object> row = child("person_documents", companyId, personId, docId);
        String type = req.documentType() == null ? (String) row.get("document_type") : upper(req.documentType());
        if (!DOCUMENT_TYPES.contains(type)) throw PersonException.badRequest("Tipo de documento inválido.");
        jdbc.update("UPDATE person_documents SET document_type = ?, document_number = ?, issuer = ?, issuer_uf = ?, issue_date = ?, "
                        + "expiry_date = ?, storage_object_id = ?, is_primary = ?, updated_at = now() WHERE id = ?",
                type, req.documentNumber() == null ? row.get("document_number") : req.documentNumber().trim(),
                blank(req.issuer()), upperBlank(req.issuerUf()), req.issueDate(), req.expiryDate(), req.storageObjectId(),
                req.primary() == null ? bool(row.get("is_primary")) : req.primary(), docId);
        history.record(companyId, personId, userId, "UPDATE", "person_documents",
                Map.of("documentType", row.get("document_type"), "documentNumber", "x"),
                Map.of("documentType", type, "documentNumber", "y"), null);
        touch(companyId, personId, userId);
        return documents(companyId, personId);
    }

    @Transactional
    public List<Document> deleteDocument(UUID companyId, UUID personId, UUID docId, UUID userId) {
        Map<String, Object> row = child("person_documents", companyId, personId, docId);
        jdbc.update("DELETE FROM person_documents WHERE id = ?", docId);
        history.record(companyId, personId, userId, "UPDATE", "person_documents",
                Map.of("documentType", row.get("document_type")), null, null);
        touch(companyId, personId, userId);
        return documents(companyId, personId);
    }

    // =================================================================== contas bancárias

    @Transactional(readOnly = true)
    public List<BankAccount> bankAccounts(UUID companyId, UUID personId) {
        requirePerson(companyId, personId);
        crypto.ensureAvailable();
        return jdbc.query("SELECT * FROM person_bank_accounts WHERE company_id = ? AND person_id = ? AND active "
                        + "ORDER BY is_primary DESC, created_at",
                (rs, i) -> new BankAccount(id(rs, "id"), rs.getString("bank_code"), crypto.decrypt(rs.getString("branch_number")),
                        crypto.decrypt(rs.getString("account_number")), crypto.decrypt(rs.getString("account_digit")),
                        rs.getString("account_type"), crypto.decrypt(rs.getString("pix_key")), rs.getString("pix_key_type"),
                        rs.getString("holder_name"), rs.getBoolean("is_primary")),
                companyId, personId);
    }

    @Transactional
    public List<BankAccount> saveBankAccount(UUID companyId, UUID personId, UUID accountId, BankAccountRequest req, UUID userId) {
        requirePerson(companyId, personId);
        if (req.accountType() != null && !ACCOUNT_TYPES.contains(req.accountType())) throw PersonException.badRequest("Tipo de conta inválido.");
        if (req.pixKeyType() != null && !PIX_TYPES.contains(req.pixKeyType())) throw PersonException.badRequest("Tipo de chave PIX inválido.");
        boolean primary = Boolean.TRUE.equals(req.primary());
        if (primary) jdbc.update("UPDATE person_bank_accounts SET is_primary = FALSE WHERE person_id = ? AND (CAST(? AS uuid) IS NULL OR id <> ?)",
                personId, accountId, accountId);
        Object[] values = {blank(req.bankCode()), crypto.encrypt(req.branchNumber()), crypto.encrypt(req.accountNumber()),
                crypto.encrypt(req.accountDigit()), req.accountType(), crypto.encrypt(req.pixKey()), req.pixKeyType(),
                blank(req.holderName()), primary};
        if (accountId == null) {
            jdbc.update("INSERT INTO person_bank_accounts (bank_code, branch_number, account_number, account_digit, account_type, "
                    + "pix_key, pix_key_type, holder_name, is_primary, company_id, person_id) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                    concat(values, companyId, personId));
        } else {
            child("person_bank_accounts", companyId, personId, accountId);
            jdbc.update("UPDATE person_bank_accounts SET bank_code = ?, branch_number = ?, account_number = ?, account_digit = ?, "
                    + "account_type = ?, pix_key = ?, pix_key_type = ?, holder_name = ?, is_primary = ?, updated_at = now() "
                    + "WHERE company_id = ? AND id = ?", concat(values, companyId, accountId));
        }
        history.record(companyId, personId, userId, "UPDATE", "person_bank_accounts", null,
                Map.of("bankCode", String.valueOf(req.bankCode()), "accountNumber", "x"), null);
        touch(companyId, personId, userId);
        return bankAccounts(companyId, personId);
    }

    @Transactional
    public List<BankAccount> deleteBankAccount(UUID companyId, UUID personId, UUID accountId, UUID userId) {
        child("person_bank_accounts", companyId, personId, accountId);
        jdbc.update("DELETE FROM person_bank_accounts WHERE id = ?", accountId);
        history.record(companyId, personId, userId, "UPDATE", "person_bank_accounts", Map.of("accountNumber", "x"), null, null);
        touch(companyId, personId, userId);
        return bankAccounts(companyId, personId);
    }

    // =================================================================== relacionamentos

    @Transactional
    public List<Relationship> addRelationship(UUID companyId, UUID personId, RelationshipRequest req, UUID userId) {
        requirePerson(companyId, personId);
        if (req.relatedPersonId() == null) throw PersonException.badRequest("Escolha a pessoa relacionada.");
        requirePerson(companyId, req.relatedPersonId());
        String type = upper(req.relationshipType());
        if (!RELATIONSHIP_TYPES.contains(type)) throw PersonException.badRequest("Tipo de relacionamento inválido.");
        boolean inverse = "IN".equalsIgnoreCase(req.direction());
        UUID subject = inverse ? req.relatedPersonId() : personId;
        UUID related = inverse ? personId : req.relatedPersonId();
        if (subject.equals(related)) throw PersonException.badRequest("Uma pessoa não pode se relacionar com ela mesma.");
        try {
            jdbc.update("INSERT INTO person_relationships (company_id, person_id, related_person_id, relationship_type, start_date, "
                            + "end_date, notes, created_by) VALUES (?,?,?,?,?,?,?,?)",
                    companyId, subject, related, type, req.startDate(), req.endDate(), blank(req.notes()), userId);
        } catch (DuplicateKeyException e) {
            throw PersonException.conflict("RELATIONSHIP_EXISTS", "Este relacionamento já existe.");
        }
        history.record(companyId, personId, userId, "UPDATE", "person_relationships", null,
                Map.of("relationship", type, "relatedPersonId", req.relatedPersonId().toString(), "direction", inverse ? "IN" : "OUT"), null);
        touch(companyId, personId, userId);
        return relationships(companyId, personId);
    }

    @Transactional
    public List<Relationship> deleteRelationship(UUID companyId, UUID personId, UUID relId, UUID userId) {
        int n = jdbc.update("DELETE FROM person_relationships WHERE company_id = ? AND id = ? AND (person_id = ? OR related_person_id = ?)",
                companyId, relId, personId, personId);
        if (n == 0) throw PersonException.notFound("Relacionamento");
        history.record(companyId, personId, userId, "UPDATE", "person_relationships", Map.of("relationshipId", relId.toString()), null, null);
        touch(companyId, personId, userId);
        return relationships(companyId, personId);
    }

    // =================================================================== leitura das filhas

    List<Address> addresses(UUID companyId, UUID personId) {
        return jdbc.query("SELECT * FROM person_addresses WHERE company_id = ? AND person_id = ? AND active "
                        + "ORDER BY is_primary DESC, created_at",
                (rs, i) -> new Address(id(rs, "id"), rs.getString("address_type"), rs.getString("postal_code"),
                        rs.getString("street"), rs.getString("address_number"), rs.getString("complement"),
                        rs.getObject("neighborhood_id", UUID.class), rs.getString("neighborhood_name"),
                        (Integer) rs.getObject("city_id"), rs.getString("city_name"), rs.getString("uf"),
                        (Integer) rs.getObject("country_id"), rs.getString("foreign_region"), rs.getBoolean("is_primary"),
                        rs.getBoolean("active")),
                companyId, personId);
    }

    List<ContactChannel> channels(UUID companyId, UUID personId) {
        return jdbc.query("SELECT * FROM person_contacts WHERE company_id = ? AND person_id = ? AND active "
                        + "ORDER BY contact_type, is_primary DESC, created_at",
                (rs, i) -> new ContactChannel(id(rs, "id"), rs.getString("contact_type"), rs.getString("value"),
                        rs.getString("contact_name"), rs.getBoolean("is_whatsapp"), rs.getBoolean("is_primary"),
                        rs.getBoolean("active")),
                companyId, personId);
    }

    List<Document> documents(UUID companyId, UUID personId) {
        return jdbc.query("SELECT * FROM person_documents WHERE company_id = ? AND person_id = ? AND active "
                        + "ORDER BY is_primary DESC, created_at",
                (rs, i) -> new Document(id(rs, "id"), rs.getString("document_type"), rs.getString("document_number"),
                        rs.getString("issuer"), rs.getString("issuer_uf"), rs.getObject("issue_date", LocalDate.class),
                        rs.getObject("expiry_date", LocalDate.class), rs.getObject("storage_object_id", UUID.class),
                        rs.getBoolean("is_primary")),
                companyId, personId);
    }

    List<Relationship> relationships(UUID companyId, UUID personId) {
        return jdbc.query("SELECT r.*, CASE WHEN r.person_id = ? THEN 'OUT' ELSE 'IN' END AS dir, "
                        + "o.id AS other_id, o.name AS other_name FROM person_relationships r "
                        + "JOIN persons o ON o.id = CASE WHEN r.person_id = ? THEN r.related_person_id ELSE r.person_id END "
                        + "WHERE r.company_id = ? AND (r.person_id = ? OR r.related_person_id = ?) ORDER BY r.created_at",
                (rs, i) -> new Relationship(id(rs, "id"), id(rs, "other_id"), rs.getString("other_name"),
                        rs.getString("relationship_type"), rs.getString("dir"), rs.getObject("start_date", LocalDate.class),
                        rs.getObject("end_date", LocalDate.class), rs.getString("notes")),
                personId, personId, companyId, personId, personId);
    }

    // =================================================================== auxiliares

    boolean isHealth(UUID companyId) {
        return modules.segmentOf(companyId) != CompanySegment.GENERIC;
    }

    boolean hasContact(UUID companyId, UUID personId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM contacts WHERE company_id = ? AND id = ?)",
                Boolean.class, companyId, personId));
    }

    void requirePerson(UUID companyId, UUID personId) {
        Boolean ok = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM persons WHERE company_id = ? AND id = ?)",
                Boolean.class, companyId, personId);
        if (!Boolean.TRUE.equals(ok)) throw PersonException.notFound("Cadastro");
    }

    private void insertContact(UUID companyId, UUID id, Normalized n, String rgIssuer, String mobile, String email,
                               AddressRequest address) {
        String[] parts = splitName(n.name);
        AddressValues a = address != null && hasAddressData(address) ? addressValues(address) : null;
        String cityName = a == null ? null : a.city;
        String uf = a == null ? null : a.uf;
        if (a != null && address.cityId() != null) {
            Map<String, Object> c = jdbc.queryForMap("SELECT c.city_name, s.uf FROM cities c JOIN states s ON s.id = c.state_id "
                    + "WHERE c.id = ?", address.cityId());
            cityName = (String) c.get("city_name");
            uf = (String) c.get("uf");
        }
        String mail = email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
        try {
            jdbc.update("INSERT INTO contacts (id, company_id, first_name, last_name, email, phone, cpf, rg, rg_issuer, birth_date, "
                            + "gender, marital_status, professional_status, notes, zip_code, street, address_number, "
                            + "address_complement, neighborhood, city, state) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    id, companyId, parts[0], parts[1], mail, mobile == null ? null : trunc(mobile.replaceAll("\\D", ""), 20),
                    n.taxId, trunc(n.rgOrIe, 20), trunc(blank(rgIssuer), 20), n.birth, n.gender, n.marital, n.professional,
                    n.notes,
                    a == null ? null : a.postal, a == null ? null : a.street, a == null ? null : a.number,
                    a == null ? null : a.complement, a == null ? null : a.neighborhood, cityName, uf);
        } catch (DuplicateKeyException e) {
            throw PersonException.conflict("EMAIL_IN_USE", "Este e-mail já está em outro cadastro.");
        }
        if (a != null) {
            jdbc.update("UPDATE person_addresses SET address_type = ?, city_id = coalesce(?, city_id), "
                    + "neighborhood_id = coalesce(?, neighborhood_id), country_id = coalesce(?, country_id) "
                    + "WHERE person_id = ? AND sync_slot = 'CONTACT_ADDRESS'",
                    a.type, address.cityId(), address.neighborhoodId(), address.countryId(), id);
        }
    }

    private boolean slotExists(UUID personId, String slot) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM person_contacts WHERE person_id = ? AND sync_slot = ?)",
                Boolean.class, personId, slot));
    }

    private void clearPrimary(String table, String typeColumn, UUID personId, String type, UUID exceptId) {
        jdbc.update("UPDATE " + table + " SET is_primary = FALSE WHERE person_id = ? AND " + typeColumn + " = ? AND is_primary "
                + "AND (CAST(? AS uuid) IS NULL OR id <> ?)", personId, type, exceptId, exceptId);
    }

    private Map<String, Object> child(String table, UUID companyId, UUID personId, UUID id) {
        List<Map<String, Object>> r = jdbc.queryForList("SELECT * FROM " + table + " WHERE company_id = ? AND person_id = ? AND id = ?",
                companyId, personId, id);
        if (r.isEmpty()) throw PersonException.notFound("Registro");
        return r.get(0);
    }

    private void touch(UUID companyId, UUID personId, UUID userId) {
        jdbc.update("UPDATE persons SET updated_at = now(), updated_by = ? WHERE company_id = ? AND id = ?", userId, companyId, personId);
    }

    private void ensureUniqueTax(UUID companyId, String tax, UUID exceptId) {
        if (tax == null) return;
        List<String> other = jdbc.queryForList("SELECT name FROM persons WHERE company_id = ? AND tax_id = ? AND NOT deleted "
                + "AND (CAST(? AS uuid) IS NULL OR id <> ?) LIMIT 1", String.class, companyId, tax, exceptId, exceptId);
        if (!other.isEmpty()) {
            throw PersonException.conflict("TAX_ID_IN_USE", "Este CPF/CNPJ já está no cadastro de " + other.get(0) + ".");
        }
    }

    Map<String, Object> snapshot(UUID companyId, UUID personId) {
        Map<String, Object> p = jdbc.queryForMap("SELECT person_type, name, trade_name, tax_id, rg_or_ie, rg_issuer, "
                + "birth_or_foundation_date, gender, marital_status, professional_status, nationality, country_id, provisional, "
                + "active, notes FROM persons WHERE company_id = ? AND id = ?", companyId, personId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("personType", p.get("person_type"));
        m.put("name", p.get("name"));
        m.put("tradeName", p.get("trade_name"));
        m.put("taxId", p.get("tax_id"));
        m.put("rgOrIe", p.get("rg_or_ie"));
        m.put("rgIssuer", p.get("rg_issuer"));
        m.put("birthOrFoundationDate", p.get("birth_or_foundation_date") == null ? null : p.get("birth_or_foundation_date").toString());
        m.put("gender", p.get("gender"));
        m.put("maritalStatus", p.get("marital_status"));
        m.put("professionalStatus", p.get("professional_status"));
        m.put("nationality", p.get("nationality"));
        m.put("countryId", p.get("country_id"));
        m.put("provisional", p.get("provisional"));
        m.put("active", p.get("active"));
        m.put("notes", p.get("notes"));
        return m;
    }

    // ---------------------------------------------------------------- normalização

    record Normalized(String type, String name, String tradeName, String taxId, String rgOrIe, boolean provisional,
                      LocalDate birth, String gender, String marital, String professional, String notes) {}

    private Normalized normalize(PersonRequest req, String currentTax) {
        String type = req.personType() == null ? "F" : upper(req.personType());
        if (!PERSON_TYPES.contains(type)) throw PersonException.badRequest("Tipo de pessoa inválido (F ou J).");
        String name = req.name() == null ? null : req.name().trim().replaceAll("\\s+", " ");
        if (name == null || name.isEmpty()) throw PersonException.badRequest("Informe o nome.");
        if (name.length() > 150) throw PersonException.badRequest("Nome com mais de 150 caracteres.");
        String tax = TaxIdValidator.normalize(req.taxId());
        if (tax != null && !tax.equals(currentTax) && !TaxIdValidator.isValid(type, tax)) {
            throw PersonException.badRequest("J".equals(type) ? "CNPJ inválido." : "CPF inválido.");
        }
        if (tax != null && tax.equals(currentTax) && !TaxIdValidator.isValid(type, tax)) {
            throw PersonException.badRequest("O documento gravado não corresponde ao tipo de pessoa escolhido.");
        }
        // Provisório só quando marcado (sem CPF/CNPJ ou estrangeiro): dispensa os campos parametrizados (RNP007).
        boolean provisional = Boolean.TRUE.equals(req.provisional()) && tax == null;
        return new Normalized(type, name, "J".equals(type) ? blank(req.tradeName()) : null, tax, blank(req.rgOrIe()),
                provisional, req.birthOrFoundationDate(), blank(req.gender()), blank(req.maritalStatus()),
                blank(req.professionalStatus()), blank(req.notes()));
    }

    private static PersonRequest withTax(PersonRequest r, String tax) {
        return new PersonRequest(r.personType(), r.name(), r.tradeName(), tax, r.rgOrIe(), r.rgIssuer(),
                r.birthOrFoundationDate(), r.gender(), r.maritalStatus(), r.professionalStatus(), r.nationality(),
                r.countryId(), r.provisional(), r.notes(), r.roles(), r.address(), r.mobile(), r.email(), r.customer(),
                r.supplier(), r.employee());
    }

    record AddressValues(String type, String postal, String street, String number, String complement, String neighborhood,
                         String city, String uf) {
        String summary() {
            return String.join(", ", java.util.stream.Stream.of(street, number, neighborhood, city, uf, postal)
                    .filter(Objects::nonNull).toList());
        }
    }

    private AddressValues addressValues(AddressRequest r) {
        String type = r.addressType() == null ? "RESIDENTIAL" : upper(r.addressType());
        if (!ADDRESS_TYPES.contains(type)) throw PersonException.badRequest("Tipo de endereço inválido.");
        String postal = r.postalCode() == null ? null : r.postalCode().replaceAll("\\D", "");
        if (postal != null && postal.isEmpty()) postal = null;
        if (postal != null && postal.length() != 8) throw PersonException.badRequest("CEP inválido.");
        return new AddressValues(type, postal, trunc(blank(r.street()), 160), trunc(blank(r.addressNumber()), 20),
                trunc(blank(r.complement()), 80), trunc(blank(r.neighborhoodName()), 80), trunc(blank(r.cityName()), 80),
                upperBlank(r.uf()));
    }

    private static String summary(Map<String, Object> a) {
        return String.join(", ", java.util.stream.Stream.of(a.get("street"), a.get("address_number"), a.get("neighborhood_name"),
                a.get("city_name"), a.get("uf"), a.get("postal_code")).filter(Objects::nonNull).map(String::valueOf).toList());
    }

    private static boolean hasAddressData(AddressRequest a) {
        return notBlank(a.postalCode()) || notBlank(a.street()) || notBlank(a.cityName()) || a.cityId() != null;
    }

    private static String channelValue(String type, String raw) {
        if (raw == null || raw.isBlank()) throw PersonException.badRequest("Informe o contato.");
        String v = raw.trim();
        if ("MOBILE".equals(type) || "PHONE".equals(type)) {
            v = v.replaceAll("\\D", "");
            if (v.length() < 8 || v.length() > 15) throw PersonException.badRequest("Telefone inválido.");
        } else if ("EMAIL".equals(type)) {
            v = v.toLowerCase(Locale.ROOT);
            if (!v.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw PersonException.badRequest("E-mail inválido.");
        }
        return v;
    }

    static String[] splitName(String name) {
        String n = name == null ? "" : name.trim();
        int i = n.indexOf(' ');
        if (i < 0) return new String[]{trunc(n, 100), null};
        return new String[]{trunc(n.substring(0, i), 100), trunc(n.substring(i + 1).trim(), 100)};
    }

    static String showTax(String tax, boolean sensitive) {
        return sensitive ? tax : TaxIdValidator.mask(tax);
    }

    static UUID id(ResultSet rs, String col) throws SQLException {
        return rs.getObject(col, UUID.class);
    }

    static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    static String upper(String s) {
        return s == null ? null : s.trim().toUpperCase(Locale.ROOT);
    }

    static String upperBlank(String s) {
        String b = blank(s);
        return b == null ? null : b.toUpperCase(Locale.ROOT);
    }

    static String trunc(String s, int max) {
        return s == null ? null : s.length() <= max ? s : s.substring(0, max);
    }

    static boolean bool(Object o) {
        return Boolean.TRUE.equals(o);
    }

    static LocalDate date(Object o) {
        if (o == null) return null;
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        if (o instanceof LocalDate d) return d;
        return LocalDate.parse(o.toString());
    }

    static OffsetDateTime odt(Object o) {
        if (o == null) return null;
        if (o instanceof OffsetDateTime d) return d;
        if (o instanceof java.sql.Timestamp t) return t.toInstant().atOffset(java.time.ZoneOffset.UTC);
        return null;
    }

    private static Object[] concat(Object[] a, Object... b) {
        Object[] r = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }
}
