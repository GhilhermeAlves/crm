package com.becommerce.crm.clinical.trichology.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Itens configuráveis por empresa: achados, marcadores e limites. */
public final class CatalogItems {

    private CatalogItems() {
    }

    public record FindingCatalogItem(UUID id, UUID companyId, String code, String label, String description,
                                     boolean active) {
    }

    /**
     * Marcador laboratorial. {@code system} agrupa no RPS (ex.: TIREOIDE, FERRO,
     * VITAMINAS, HORMONIOS, INFLAMACAO). {@code sex} nulo vale para ambos.
     */
    public record Marker(UUID id, UUID companyId, String code, String label, String unit, String system,
                         String sex, BigDecimal referenceMin, BigDecimal referenceMax, Integer targetQuartile,
                         boolean active) {
    }

    /** Uma regra da clínica: hipótese → exames a sugerir. */
    public record ExamRule(String hypothesis, List<String> markerCodes) {
    }

    public record Settings(UUID companyId, BigDecimal minConfidence, List<Integer> reportHorizonsMonths,
                           List<ExamRule> examRules) {
    }

    /** Catálogo inicial de achados (doc v1.2, seção 3.2). */
    public static List<String[]> defaultFindings() {
        return List.of(
                new String[]{"MINIATURIZATION", "Miniaturização de fios"},
                new String[]{"DIAMETER_VARIATION", "Variação de diâmetro dos fios > 20%"},
                new String[]{"YELLOW_DOTS", "Pontos amarelos"},
                new String[]{"BLACK_DOTS", "Pontos pretos"},
                new String[]{"EXCLAMATION_MARK_HAIRS", "Pelos em ponto de exclamação"},
                new String[]{"SCALING", "Descamação"},
                new String[]{"PERIFOLLICULAR_ERYTHEMA", "Eritema perifolicular"},
                new String[]{"PERIPILAR_SIGN", "Halo peripilar"},
                new String[]{"BROKEN_HAIRS", "Fios quebrados"},
                new String[]{"SINGLE_HAIR_UNITS", "Unidades foliculares com fio único"},
                new String[]{"VELLUS_HAIRS", "Pelos velus"},
                new String[]{"UPRIGHT_REGROWING_HAIRS", "Fios em recrescimento"},
                new String[]{"PERIFOLLICULAR_SCALING", "Descamação perifolicular"},
                new String[]{"LOSS_OF_FOLLICULAR_OPENINGS", "Perda de óstios foliculares"},
                new String[]{"HONEYCOMB_PIGMENTATION", "Pigmentação em favo de mel"},
                new String[]{"ARBORIZING_VESSELS", "Vasos arboriformes"});
    }

    /**
     * Marcadores iniciais. Faixas são ponto de partida editável pela clínica
     * (decisão clínica em aberto, doc v1.2 seção 13).
     */
    public static List<Object[]> defaultMarkers() {
        return List.of(
                new Object[]{"FERRITIN", "Ferritina", "ng/mL", "FERRO", "F", "15", "150", 3},
                new Object[]{"FERRITIN", "Ferritina", "ng/mL", "FERRO", "M", "30", "400", 3},
                new Object[]{"SERUM_IRON", "Ferro sérico", "µg/dL", "FERRO", null, "60", "170", 3},
                new Object[]{"TRANSFERRIN_SATURATION", "Saturação de transferrina", "%", "FERRO", null, "20", "50", 3},
                new Object[]{"HEMOGLOBIN", "Hemoglobina", "g/dL", "HEMATOLOGIA", "F", "12", "16", 3},
                new Object[]{"HEMOGLOBIN", "Hemoglobina", "g/dL", "HEMATOLOGIA", "M", "13.5", "17.5", 3},
                new Object[]{"TSH", "TSH", "µUI/mL", "TIREOIDE", null, "0.4", "4.5", 2},
                new Object[]{"FREE_T4", "T4 livre", "ng/dL", "TIREOIDE", null, "0.8", "1.8", 3},
                new Object[]{"VITAMIN_D", "Vitamina D (25-OH)", "ng/mL", "VITAMINAS", null, "30", "100", 3},
                new Object[]{"VITAMIN_B12", "Vitamina B12", "pg/mL", "VITAMINAS", null, "200", "900", 3},
                new Object[]{"FOLATE", "Ácido fólico", "ng/mL", "VITAMINAS", null, "3", "17", 3},
                new Object[]{"ZINC", "Zinco", "µg/dL", "MINERAIS", null, "70", "120", 3},
                new Object[]{"TOTAL_TESTOSTERONE", "Testosterona total", "ng/dL", "HORMONIOS", "F", "8", "60", 2},
                new Object[]{"TOTAL_TESTOSTERONE", "Testosterona total", "ng/dL", "HORMONIOS", "M", "300", "1000", 3},
                new Object[]{"DHEA_S", "DHEA-S", "µg/dL", "HORMONIOS", "F", "35", "430", 2},
                new Object[]{"FASTING_INSULIN", "Insulina de jejum", "µUI/mL", "METABOLISMO", null, "2", "25", 1},
                new Object[]{"FASTING_GLUCOSE", "Glicemia de jejum", "mg/dL", "METABOLISMO", null, "70", "99", 2},
                new Object[]{"CRP", "Proteína C reativa ultrassensível", "mg/L", "INFLAMACAO", null, "0", "3", 1});
    }

    /** Regras iniciais hipótese → exames. */
    public static List<ExamRule> defaultExamRules() {
        return List.of(
                new ExamRule("TELOGEN_EFFLUVIUM", List.of("FERRITIN", "SERUM_IRON", "TRANSFERRIN_SATURATION",
                        "HEMOGLOBIN", "TSH", "FREE_T4", "VITAMIN_D", "VITAMIN_B12", "FOLATE", "ZINC")),
                new ExamRule("ANDROGENETIC_PATTERN", List.of("TOTAL_TESTOSTERONE", "DHEA_S", "FASTING_INSULIN",
                        "FASTING_GLUCOSE", "FERRITIN", "VITAMIN_D")),
                new ExamRule("INFLAMMATORY_PATTERN", List.of("CRP", "TSH", "VITAMIN_D", "FERRITIN")));
    }
}
