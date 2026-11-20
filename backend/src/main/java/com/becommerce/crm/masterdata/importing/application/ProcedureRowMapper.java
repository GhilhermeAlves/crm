package com.becommerce.crm.masterdata.importing.application;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/** Converte uma linha de procedures.csv (exportação de outro sistema) em procedimento do odontograma. */
public final class ProcedureRowMapper {

    /** Dente (FDI) e faces (M, D, O, V, L). */
    public record Tooth(int number, List<String> faces) {}

    public record Mapped(int line, String sourceId, String sourcePatientId, String sourceTypeId,
                         String state, LocalDate performedAt, long priceCents, String notes,
                         String dentition, List<Tooth> teeth, boolean discarded,
                         String error, List<String> warnings) {}

    private static final Map<String, String> STATE = Map.of(
            "pending", "PLANNED", "done", "DONE", "existed", "PRE_EXISTING");
    private static final Map<String, String> FACE = Map.of(
            "mesial", "M", "distal", "D", "occlusal", "O", "incisal", "O",
            "vestibular", "V", "buccal", "V", "lingual", "L", "palatal", "L");
    private static final Map<String, String> ARCH = Map.of(
            "entire_arch", "Arcada inteira", "upper_arch", "Arcada superior", "lower_arch", "Arcada inferior");

    private ProcedureRowMapper() {}

    public static Mapped map(int line, Map<String, String> row) {
        List<String> warnings = new ArrayList<>();
        String error = null;

        String status = val(row, "status");
        String state = status == null ? null : STATE.get(status.toLowerCase(Locale.ROOT));
        if (state == null) error = "Situação \"" + status + "\" desconhecida";

        LocalDate performed = date(val(row, "done_at"));
        if ("DONE".equals(state) && performed == null) {
            performed = date(val(row, "updated_at"));
            if (performed == null) performed = date(val(row, "created_at"));
            warnings.add("Sem data de realização; usada a data do registro");
        }
        if (!"DONE".equals(state)) performed = null;

        long price = 0;
        String amount = val(row, "amount");
        if (amount != null) {
            try {
                price = Math.max(0, Long.parseLong(amount));
            } catch (NumberFormatException e) {
                warnings.add("Valor \"" + amount + "\" ignorado");
            }
        }

        List<String> notes = new ArrayList<>();
        String obs = val(row, "observations");
        if (obs != null) notes.add(obs);

        List<Tooth> teeth = new ArrayList<>();
        String dentition = "PERMANENT";
        String tooth = val(row, "tooth");
        if (tooth != null) {
            String arch = ARCH.get(tooth.toLowerCase(Locale.ROOT));
            if (arch != null) {
                notes.add(0, arch);
            } else {
                try {
                    int n = Integer.parseInt(tooth);
                    if (!validTooth(n)) throw new NumberFormatException();
                    if (n >= 51) dentition = "DECIDUOUS";
                    teeth.add(new Tooth(n, faces(val(row, "tooth_surfaces"), warnings)));
                } catch (NumberFormatException e) {
                    error = error != null ? error : "Dente \"" + tooth + "\" inválido";
                }
            }
        }

        return new Mapped(line, val(row, "id"), val(row, "patient_id"), val(row, "procedure_type_id"),
                state, performed, price, notes.isEmpty() ? null : String.join(" · ", notes),
                dentition, List.copyOf(teeth), val(row, "discarded_at") != null, error, List.copyOf(warnings));
    }

    private static boolean validTooth(int n) {
        int q = n / 10, d = n % 10;
        return (q >= 1 && q <= 4 && d >= 1 && d <= 8) || (q >= 5 && q <= 8 && d >= 1 && d <= 5);
    }

    private static List<String> faces(String raw, List<String> warnings) {
        if (raw == null) return List.of();
        Set<String> out = new LinkedHashSet<>();
        for (String f : raw.replace("{", "").replace("}", "").split(",")) {
            String key = f.trim().toLowerCase(Locale.ROOT);
            if (key.isEmpty()) continue;
            String mapped = FACE.get(key);
            if (mapped == null) warnings.add("Face \"" + key + "\" ignorada");
            else out.add(mapped);
        }
        return List.copyOf(out);
    }

    private static LocalDate date(String v) {
        if (v == null || v.length() < 10) return null;
        try {
            return LocalDate.parse(v.substring(0, 10));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String val(Map<String, String> row, String key) {
        String v = row.get(key);
        if (v == null) return null;
        v = v.trim();
        return v.isEmpty() ? null : v;
    }
}
