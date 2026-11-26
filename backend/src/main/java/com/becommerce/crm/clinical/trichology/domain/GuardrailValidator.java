package com.becommerce.crm.clinical.trichology.domain;

import com.becommerce.crm.clinical.trichology.domain.AssessmentData.ReportSection;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Regras determinísticas que barram conteúdo da IA (doc v1.2, seção 3.4).
 * Devolve a lista de violações; vazia significa liberado.
 */
public final class GuardrailValidator {

    /** Termos de diagnóstico fechado ou prescrição que a IA não pode afirmar. */
    private static final List<Pattern> FORBIDDEN = List.of(
            Pattern.compile("\\bdiagnostico (e|de|confirmado)\\b"),
            Pattern.compile("\\bo paciente (tem|possui|apresenta diagnostico)\\b"),
            Pattern.compile("\\bprescrevo\\b"),
            Pattern.compile("\\btomar \\d+ ?(mg|comprimid)"),
            Pattern.compile("\\bcura(r|do)?\\b"));

    private static final Pattern TRICHOSCOPY_CLAIM =
            Pattern.compile("\\btricoscopia (realizada|mostrou|evidenciou|revelou)|\\bna tricoscopia\\b");

    private GuardrailValidator() {
    }

    /** Achados propostos pela IA para uma imagem. */
    public static List<String> validateFindings(List<Finding> findings, Set<String> catalogCodes,
                                                String imageQuality) {
        List<String> violations = new ArrayList<>();
        if (imageQuality != null && !"OK".equals(imageQuality) && findings != null && !findings.isEmpty()) {
            violations.add("Imagem de baixa qualidade não pode gerar achados.");
        }
        for (Finding f : findings == null ? List.<Finding>of() : findings) {
            if (f.code() == null || !catalogCodes.contains(f.code())) {
                violations.add("Achado fora do catálogo: " + f.code());
            }
            if (f.confidence() != null && (f.confidence() < 0 || f.confidence() > 1)) {
                violations.add("Confiança inválida no achado " + f.code());
            }
            if (f.severity() != null && !Set.of("MILD", "MODERATE", "SEVERE").contains(f.severity())) {
                violations.add("Intensidade inválida no achado " + f.code());
            }
        }
        return violations;
    }

    /** Estimativas numéricas fora da faixa plausível são descartadas (não bloqueiam a foto). */
    public static boolean plausible(String key, BigDecimal value) {
        BigDecimal[] range = AssessmentImage.MEASURE_RANGES.get(key);
        return range != null && value != null && value.compareTo(range[0]) >= 0 && value.compareTo(range[1]) <= 0;
    }

    /**
     * Seções do relatório: proibe diagnóstico fechado/prescrição em texto da IA
     * e afirmação de tricoscopia quando ela não foi feita.
     */
    public static List<String> validateReport(List<ReportSection> sections, boolean trichoscopyPerformed) {
        List<String> violations = new ArrayList<>();
        for (ReportSection section : sections) {
            if (section.content() == null) {
                continue;
            }
            String text = fold(section.content());
            if (ReportSection.AI.equals(section.origin())) {
                for (Pattern p : FORBIDDEN) {
                    if (p.matcher(text).find()) {
                        violations.add("Seção \"" + section.title() + "\": linguagem de diagnóstico ou prescrição.");
                        break;
                    }
                }
            }
            if (!trichoscopyPerformed && TRICHOSCOPY_CLAIM.matcher(text).find()) {
                violations.add("Seção \"" + section.title() + "\" cita tricoscopia, mas ela não foi realizada.");
            }
        }
        return violations;
    }

    static String fold(String text) {
        String n = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.toLowerCase(Locale.ROOT);
    }
}
