package com.becommerce.crm.masterdata.person;

import com.becommerce.crm.masterdata.person.PersonDtos.FieldSetting;
import com.becommerce.crm.masterdata.person.PersonDtos.FieldSettingChange;
import com.becommerce.crm.masterdata.person.PersonDtos.FieldSettings;
import com.becommerce.crm.masterdata.person.PersonException.MissingField;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Obrigatoriedade de campos por empresa e contexto (doc seção 18). Valor efetivo = exceção da
 * empresa (company_person_field_settings) ou o padrão do catálogo. A validação lê o estado já
 * gravado da pessoa, na mesma transação: se faltar campo, lança 422 e tudo é desfeito.
 */
@Service
public class PersonFieldSettingsService {

    public static final List<String> CONTEXTS = List.of("PF", "PJ", "CUSTOMER", "SUPPLIER", "EMPLOYEE");

    private final JdbcTemplate jdbc;
    private final PersonHistoryService history;

    public PersonFieldSettingsService(JdbcTemplate jdbc, PersonHistoryService history) {
        this.jdbc = jdbc;
        this.history = history;
    }

    @Transactional(readOnly = true)
    public FieldSettings get(UUID companyId) {
        Map<String, FieldSetting> fields = new LinkedHashMap<>();
        jdbc.query("SELECT f.field_key, f.label, f.group_name, f.system_required, f.sort_order, fc.context, "
                        + "coalesce(s.required, fc.default_required) AS req "
                        + "FROM person_fields f JOIN person_field_contexts fc ON fc.field_id = f.id "
                        + "LEFT JOIN company_person_field_settings s ON s.field_id = fc.field_id "
                        + "  AND s.context = fc.context AND s.company_id = ? "
                        + "WHERE f.active ORDER BY f.group_name, f.sort_order",
                rs -> {
                    String key = rs.getString("field_key");
                    FieldSetting fs = fields.computeIfAbsent(key, k -> {
                        try {
                            Map<String, Boolean> ctx = new LinkedHashMap<>();
                            for (String c : CONTEXTS) ctx.put(c, null);
                            return new FieldSetting(k, rs.getString("label"), rs.getString("group_name"),
                                    rs.getBoolean("system_required"), rs.getInt("sort_order"), ctx);
                        } catch (java.sql.SQLException e) {
                            throw new IllegalStateException(e);
                        }
                    });
                    fs.contexts().put(rs.getString("context"), fs.systemRequired() || rs.getBoolean("req"));
                }, companyId);
        List<Map<String, Object>> last = jdbc.queryForList(
                "SELECT s.updated_at, nullif(trim(coalesce(u.first_name,'') || ' ' || coalesce(u.last_name,'')), '') AS n "
                        + "FROM company_person_field_settings s LEFT JOIN users u ON u.id = s.updated_by "
                        + "WHERE s.company_id = ? AND s.updated_by IS NOT NULL ORDER BY s.updated_at DESC LIMIT 1",
                companyId);
        OffsetDateTime at = last.isEmpty() ? null : toOffset(last.get(0).get("updated_at"));
        String by = last.isEmpty() ? null : (String) last.get(0).get("n");
        return new FieldSettings(List.copyOf(fields.values()), at, by);
    }

    @Transactional
    public FieldSettings update(UUID companyId, Collection<FieldSettingChange> changes, UUID userId) {
        for (FieldSettingChange ch : changes) {
            if (!CONTEXTS.contains(ch.context())) throw PersonException.badRequest("Contexto inválido: " + ch.context());
            List<Map<String, Object>> f = jdbc.queryForList(
                    "SELECT f.id, f.system_required FROM person_fields f JOIN person_field_contexts fc "
                            + "ON fc.field_id = f.id AND fc.context = ? WHERE f.field_key = ?",
                    ch.context(), ch.fieldKey());
            if (f.isEmpty()) throw PersonException.badRequest("Campo não se aplica ao contexto: " + ch.fieldKey());
            if (Boolean.TRUE.equals(f.get(0).get("system_required")) && !ch.required()) {
                throw PersonException.badRequest("Campo obrigatório pelo sistema não pode ser desmarcado.");
            }
            jdbc.update("INSERT INTO company_person_field_settings (company_id, field_id, context, required, updated_by) "
                            + "VALUES (?, ?, ?, ?, ?) ON CONFLICT (company_id, field_id, context) DO UPDATE SET "
                            + "required = EXCLUDED.required, updated_by = EXCLUDED.updated_by, updated_at = now(), "
                            + "version = company_person_field_settings.version + 1",
                    companyId, f.get(0).get("id"), ch.context(), ch.required(), userId);
        }
        return get(companyId);
    }

    @Transactional
    public FieldSettings resetToDefault(UUID companyId) {
        jdbc.update("DELETE FROM company_person_field_settings WHERE company_id = ?", companyId);
        return get(companyId);
    }

    /** Campos obrigatórios efetivos para os contextos (união). */
    public Map<String, String> required(UUID companyId, Collection<String> contexts) {
        if (contexts.isEmpty()) return Map.of();
        Map<String, String> out = new LinkedHashMap<>();
        String in = String.join(",", contexts.stream().map(c -> "'" + c.replace("'", "") + "'").toList());
        jdbc.query("SELECT DISTINCT f.field_key, f.label FROM person_fields f "
                        + "JOIN person_field_contexts fc ON fc.field_id = f.id "
                        + "LEFT JOIN company_person_field_settings s ON s.field_id = fc.field_id AND s.context = fc.context "
                        + "  AND s.company_id = ? "
                        + "WHERE f.active AND fc.context IN (" + in + ") "
                        + "AND (f.system_required OR coalesce(s.required, fc.default_required))",
                rs -> {
                    out.put(rs.getString("field_key"), rs.getString("label"));
                }, companyId);
        return out;
    }

    /**
     * Valida a pessoa gravada contra os contextos: tipo (PF/PJ) + papéis informados.
     * Provisória (RNP007): só os campos do sistema. Estrangeira (RNP008): sem CPF/CEP/UF/cidade.
     */
    public void validate(UUID companyId, UUID personId, Collection<String> roleContexts) {
        Map<String, Object> p = jdbc.queryForMap("SELECT person_type, tax_id, rg_or_ie, birth_or_foundation_date, "
                + "trade_name, provisional, name FROM persons WHERE company_id = ? AND id = ?", companyId, personId);
        Set<String> ctx = new LinkedHashSet<>();
        ctx.add("J".equals(p.get("person_type")) ? "PJ" : "PF");
        ctx.addAll(roleContexts);
        Map<String, String> req = required(companyId, ctx);
        boolean provisional = Boolean.TRUE.equals(p.get("provisional"));
        Map<String, Object> addr = jdbc.queryForList("SELECT postal_code, street, address_number, neighborhood_name, "
                + "city_name, uf, country_id FROM person_addresses WHERE company_id = ? AND person_id = ? AND active "
                + "ORDER BY is_primary DESC, created_at LIMIT 1", companyId, personId).stream().findFirst().orElse(Map.of());
        Object country = addr.get("country_id");
        boolean foreign = country != null && ((Number) country).intValue() != 1;
        Set<String> types = new LinkedHashSet<>(jdbc.queryForList(
                "SELECT contact_type FROM person_contacts WHERE company_id = ? AND person_id = ? AND active",
                String.class, companyId, personId));
        boolean whatsapp = Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM person_contacts WHERE company_id = ? AND person_id = ? AND active AND is_whatsapp)",
                Boolean.class, companyId, personId));

        List<MissingField> missing = new ArrayList<>();
        for (Map.Entry<String, String> e : req.entrySet()) {
            String k = e.getKey();
            if (provisional && !"PERSON_NAME".equals(k)) continue;
            boolean ok = switch (k) {
                case "PERSON_NAME" -> notBlank(p.get("name"));
                case "PERSON_TAX_ID" -> foreign || notBlank(p.get("tax_id"));
                case "PERSON_RG_IE" -> notBlank(p.get("rg_or_ie"));
                case "PERSON_BIRTH_DATE" -> p.get("birth_or_foundation_date") != null;
                case "PERSON_TRADE_NAME" -> !"J".equals(p.get("person_type")) || notBlank(p.get("trade_name"));
                case "PERSON_POSTAL_CODE" -> foreign || notBlank(addr.get("postal_code"));
                case "PERSON_STREET" -> notBlank(addr.get("street"));
                case "PERSON_ADDRESS_NUMBER" -> notBlank(addr.get("address_number"));
                case "PERSON_NEIGHBORHOOD" -> foreign || notBlank(addr.get("neighborhood_name"));
                case "PERSON_CITY" -> foreign || notBlank(addr.get("city_name"));
                case "PERSON_STATE" -> foreign || notBlank(addr.get("uf"));
                case "PERSON_COUNTRY" -> addr.isEmpty() || country != null || notBlank(addr.get("uf"));
                case "PERSON_PHONE" -> types.contains("PHONE");
                case "PERSON_MOBILE" -> types.contains("MOBILE");
                case "PERSON_EMAIL" -> types.contains("EMAIL");
                case "PERSON_WHATSAPP" -> whatsapp;
                case "PERSON_BANK_ACCOUNT" -> exists("person_bank_accounts", companyId, personId);
                case "PERSON_DOCUMENT" -> exists("person_documents", companyId, personId);
                case "EMPLOYEE_JOB_TITLE" -> !roleContexts.contains("EMPLOYEE") || notNullCol(
                        "SELECT job_title_id FROM employees WHERE company_id = ? AND person_id = ?", companyId, personId);
                case "EMPLOYEE_HIRE_DATE" -> !roleContexts.contains("EMPLOYEE") || notNullCol(
                        "SELECT hire_date FROM employees WHERE company_id = ? AND person_id = ?", companyId, personId);
                case "EMPLOYEE_SPECIALTY" -> !roleContexts.contains("EMPLOYEE") || exists("employee_specialties", companyId, personId);
                case "EMPLOYEE_COUNCIL" -> !roleContexts.contains("EMPLOYEE") || Boolean.TRUE.equals(jdbc.queryForObject(
                        "SELECT EXISTS (SELECT 1 FROM employee_specialties WHERE company_id = ? AND employee_id = ? "
                                + "AND council_number IS NOT NULL)", Boolean.class, companyId, personId));
                case "PATIENT_RECORD_NUMBER" -> patientField("record_number", companyId, personId);
                case "PATIENT_HEALTH_PLAN" -> patientField("health_plan", companyId, personId);
                case "PATIENT_REFERRAL_SOURCE" -> patientField("referral_source", companyId, personId);
                case "PATIENT_GUARDIAN_MINOR" -> guardianOk(companyId, personId, (LocalDate) toLocalDate(p.get("birth_or_foundation_date")));
                default -> true;
            };
            if (!ok) missing.add(new MissingField(k, e.getValue()));
        }
        if (!missing.isEmpty()) {
            throw new PersonException(HttpStatus.UNPROCESSABLE_ENTITY, "REQUIRED_FIELDS",
                    "Preencha os campos obrigatórios: " + String.join(", ", missing.stream().map(MissingField::label).toList()) + ".",
                    missing);
        }
    }

    private boolean patientField(String column, UUID companyId, UUID personId) {
        List<Map<String, Object>> r = jdbc.queryForList("SELECT " + column + " AS v FROM patients WHERE company_id = ? AND person_id = ?",
                companyId, personId);
        return r.isEmpty() || notBlank(r.get(0).get("v"));
    }

    private boolean guardianOk(UUID companyId, UUID personId, LocalDate birth) {
        if (birth == null || Period.between(birth, LocalDate.now()).getYears() >= 18) return true;
        Boolean patient = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM customers WHERE company_id = ? AND person_id = ?)",
                Boolean.class, companyId, personId);
        if (!Boolean.TRUE.equals(patient)) return true;
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM person_relationships WHERE company_id = ? "
                + "AND person_id = ? AND relationship_type = 'GUARDIAN')", Boolean.class, companyId, personId));
    }

    private boolean exists(String table, UUID companyId, UUID personId) {
        String col = table.startsWith("employee_") ? "employee_id" : "person_id";
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM " + table + " WHERE company_id = ? AND "
                + col + " = ?)", Boolean.class, companyId, personId));
    }

    private boolean notNullCol(String sql, UUID companyId, UUID personId) {
        List<Map<String, Object>> r = jdbc.queryForList(sql, companyId, personId);
        return !r.isEmpty() && r.get(0).values().iterator().next() != null;
    }

    private static boolean notBlank(Object o) {
        return o != null && !String.valueOf(o).isBlank();
    }

    private static Object toLocalDate(Object o) {
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        return o;
    }

    private static OffsetDateTime toOffset(Object o) {
        if (o instanceof OffsetDateTime odt) return odt;
        if (o instanceof java.sql.Timestamp ts) return ts.toInstant().atOffset(java.time.ZoneOffset.UTC);
        return null;
    }
}
