package com.becommerce.crm.clinical.trichology.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Estruturas gravadas em JSONB no exame. */
public final class AssessmentData {

    private AssessmentData() {
    }

    /**
     * Pull test por região: fios soltos em uma tração de ~60 fios.
     * Positivo quando mais de 6 fios saem (critério usual).
     */
    public record PullTest(List<PullTestSite> sites, String notes) {
        public boolean positive() {
            return sites != null && sites.stream().anyMatch(PullTestSite::positive);
        }
    }

    public record PullTestSite(String regionCode, int hairsPulled) {
        public static final int POSITIVE_THRESHOLD = 6;

        public boolean positive() {
            return hairsPulled > POSITIVE_THRESHOLD;
        }
    }

    /** Região do protocolo pulada na captura, com o motivo. */
    public record SkippedRegion(String regionCode, String reason) {
    }

    public record LabResult(String markerCode, BigDecimal value, String unit) {
    }

    /** Resultado do RPS por marcador: quartil dentro da faixa e pontuação 0–100. */
    public record MarkerScore(String markerCode, String label, String system, BigDecimal value, String unit,
                              BigDecimal referenceMin, BigDecimal referenceMax, Integer quartile,
                              Integer targetQuartile, String position, int score) {
    }

    public record SystemScore(String system, int score, int markers) {
    }

    /** RPS = média das notas por sistema. Calculado no código, nunca pela IA. */
    public record RpsSummary(int overallScore, List<SystemScore> systems, List<MarkerScore> markers,
                             List<String> unknownMarkers) {
    }

    /**
     * Seção do relatório. {@code origin}: CODE (montada pelo sistema), AI
     * (rascunho da IA ainda não editado) ou EDITED (texto do profissional).
     */
    public record ReportSection(String key, String title, String content, String origin) {
        public static final String CODE = "CODE";
        public static final String AI = "AI";
        public static final String EDITED = "EDITED";
    }

    /**
     * Relatório inteiro. {@code facts} guarda os dados que o código montou
     * (procedimentos feitos/não feitos, quadro por região, RPS, evolução) para o
     * PDF não depender de texto da IA nos números.
     */
    public record ReportPayload(List<ReportSection> sections, Map<String, Object> facts,
                                List<String> guardrailViolations) {
    }
}
