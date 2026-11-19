package com.becommerce.crm.masterdata.importing.application;

import com.becommerce.crm.masterdata.contact.application.dto.request.CreateContactRequest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/** Converte uma linha de patients.csv (exportação de outro sistema) em CreateContactRequest. */
public final class PatientRowMapper {

    private static final Map<String, String> GENDER = Map.of(
            "male", "MASCULINO", "m", "MASCULINO", "masculino", "MASCULINO",
            "female", "FEMININO", "f", "FEMININO", "feminino", "FEMININO",
            "other", "OUTRO", "outro", "OUTRO");
    private static final Map<String, String> MARITAL = Map.ofEntries(
            Map.entry("single", "SOLTEIRO"), Map.entry("solteiro", "SOLTEIRO"),
            Map.entry("married", "CASADO"), Map.entry("casado", "CASADO"),
            Map.entry("divorced", "DIVORCIADO"), Map.entry("divorciado", "DIVORCIADO"),
            Map.entry("widowed", "VIUVO"), Map.entry("viuvo", "VIUVO"),
            Map.entry("stable_union", "UNIAO_ESTAVEL"), Map.entry("uniao_estavel", "UNIAO_ESTAVEL"));

    private PatientRowMapper() {}

    public static MappedPatient map(int line, Map<String, String> row) {
        List<String> warnings = new ArrayList<>();
        Fields f = new Fields(row, warnings);

        String fullName = f.raw("name") == null ? "" : f.raw("name").replaceAll("\\s+", " ");
        int sp = fullName.indexOf(' ');
        String first = sp < 0 ? fullName : fullName.substring(0, sp);
        String last = sp < 0 ? null : fullName.substring(sp + 1);

        String email = f.raw("email") == null ? null : f.raw("email").toLowerCase(Locale.ROOT);
        String gender = f.lookup("gender", GENDER);
        String marital = f.lookup("marital_status", MARITAL);
        String state = f.raw("state") == null ? null : f.raw("state").toUpperCase(Locale.ROOT);
        if (state != null && !state.matches("[A-Z]{2}")) {
            warnings.add("UF \"" + state + "\" ignorada");
            state = null;
        }
        LocalDate birth = null;
        if (f.raw("birthdate") != null) {
            try {
                birth = LocalDate.parse(f.raw("birthdate").substring(0, Math.min(10, f.raw("birthdate").length())));
            } catch (DateTimeParseException e) {
                warnings.add("Data de nascimento \"" + f.raw("birthdate") + "\" ignorada");
            }
        }
        String occupation = f.raw("occupation");
        String guardianName = f.cut("contact_name", 160);

        CreateContactRequest req = new CreateContactRequest(
                f.cut(first.isEmpty() ? null : first, "name", 100), f.cut(last, "name", 100),
                f.cut(email, "email", 255),
                f.cut(f.digits("local_phone"), "local_phone", 20),
                f.cut(f.digits("phone"), "phone", 20),
                occupation == null ? null : f.cut("Profissão: " + occupation, "occupation", 500),
                birth,
                f.cut(f.digits("cpf"), "cpf", 14),
                f.cut("identity_document", 20), f.cut("identity_document_emissor", 20),
                gender, marital, null,
                f.cut(f.digits("zipcode"), "zipcode", 10),
                f.cut("address1", 160), f.cut("address_number", 20), f.cut("address2", 80),
                f.cut("district", 80), f.cut("city", 80), state,
                f.cut("health_plan", 80), f.cut("health_plan_number", 40),
                f.cut("medical_record_id", 40), f.cut("source", 80),
                guardianName == null ? null : Boolean.TRUE, guardianName,
                f.cut(f.digits("contact_cpf"), "contact_cpf", 14),
                f.cut(f.digits("contact_phone"), "contact_phone", 20));

        return new MappedPatient(line, fullName, req, f.raw("discarded_at") != null, List.copyOf(warnings));
    }

    private record Fields(Map<String, String> row, List<String> warnings) {
        String raw(String key) {
            String v = row.get(key);
            if (v == null) return null;
            v = v.trim();
            return v.isEmpty() ? null : v;
        }
        String digits(String key) {
            String v = raw(key);
            if (v == null) return null;
            v = v.replaceAll("\\D", "");
            return v.isEmpty() ? null : v;
        }
        String cut(String key, int max) { return cut(raw(key), key, max); }
        String cut(String value, String key, int max) {
            if (value == null || value.length() <= max) return value;
            warnings.add("Campo " + key + " cortado em " + max + " caracteres");
            return value.substring(0, max);
        }
        String lookup(String key, Map<String, String> table) {
            String v = raw(key);
            if (v == null) return null;
            String mapped = table.get(v.toLowerCase(Locale.ROOT));
            if (mapped == null) warnings.add("Valor \"" + v + "\" de " + key + " ignorado");
            return mapped;
        }
    }
}
