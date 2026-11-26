package com.becommerce.crm.clinical.trichology.domain;

import com.becommerce.crm.clinical.trichology.domain.exception.TrichologyValidationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Foto do tricoscópio ligada a um exame, com região, análise da IA e revisão do
 * profissional. Estimativas da IA ({@code aiEstimates}) só viram medida em
 * {@code confirmedMeasures} quando o profissional confirma.
 */
public record AssessmentImage(UUID id, UUID companyId, UUID assessmentId, UUID imageId, String regionCode,
                              String magnification, String qualityStatus, String aiStatus, String aiImageQuality,
                              List<Finding> aiFindings, Map<String, BigDecimal> aiEstimates, String aiNotes,
                              List<Finding> acceptedFindings, Map<String, BigDecimal> confirmedMeasures,
                              boolean includeInReport, UUID reviewedBy, Instant reviewedAt, Instant createdAt,
                              Instant updatedAt) {

    public static final String AI_PENDING = "PENDING";
    public static final String AI_RUNNING = "RUNNING";
    public static final String AI_DONE = "DONE";
    public static final String AI_FAILED = "FAILED";
    public static final String AI_SKIPPED = "SKIPPED";

    /** Medidas aceitas e faixas plausíveis (guardrail das estimativas). */
    public static final Map<String, BigDecimal[]> MEASURE_RANGES = Map.of(
            "hairDensityPerCm2", new BigDecimal[]{BigDecimal.ZERO, new BigDecimal("400")},
            "thinHairPercent", new BigDecimal[]{BigDecimal.ZERO, new BigDecimal("100")},
            "singleHairFollicularUnitsPercent", new BigDecimal[]{BigDecimal.ZERO, new BigDecimal("100")},
            "meanShaftDiameterMicrons", new BigDecimal[]{BigDecimal.ZERO, new BigDecimal("150")});

    private static final List<String> QUALITY = List.of("UNCHECKED", "OK", "BLURRED", "DARK", "GLARE");

    public static AssessmentImage link(UUID companyId, UUID assessmentId, UUID imageId, String regionCode,
                                       String magnification, String qualityStatus, boolean aiEnabled,
                                       Instant now) {
        String quality = qualityStatus == null ? "UNCHECKED" : qualityStatus;
        if (!QUALITY.contains(quality)) {
            throw new TrichologyValidationException("Qualidade da imagem inválida: " + qualityStatus);
        }
        return new AssessmentImage(UUID.randomUUID(), companyId, assessmentId, imageId, regionCode, magnification,
                quality, aiEnabled ? AI_PENDING : AI_SKIPPED, null, List.of(), Map.of(), null, List.of(), Map.of(),
                true, null, null, now, now);
    }

    public AssessmentImage withAiStatus(String status, Instant now) {
        return new AssessmentImage(id, companyId, assessmentId, imageId, regionCode, magnification, qualityStatus,
                status, aiImageQuality, aiFindings, aiEstimates, aiNotes, acceptedFindings, confirmedMeasures,
                includeInReport, reviewedBy, reviewedAt, createdAt, now);
    }

    public AssessmentImage withAnalysis(String imageQuality, List<Finding> findings, Map<String, BigDecimal> estimates,
                                        String notes, Instant now) {
        return new AssessmentImage(id, companyId, assessmentId, imageId, regionCode, magnification, qualityStatus,
                AI_DONE, imageQuality, List.copyOf(findings), Map.copyOf(estimates), notes, acceptedFindings,
                confirmedMeasures, includeInReport, reviewedBy, reviewedAt, createdAt, now);
    }

    public AssessmentImage reviewed(List<Finding> accepted, Map<String, BigDecimal> measures, boolean include,
                                    UUID userId, Instant now) {
        Map<String, BigDecimal> m = measures == null ? Map.of() : measures;
        m.forEach((key, value) -> {
            BigDecimal[] range = MEASURE_RANGES.get(key);
            if (range == null) {
                throw new TrichologyValidationException("Medida desconhecida: " + key);
            }
            if (value == null || value.compareTo(range[0]) < 0 || value.compareTo(range[1]) > 0) {
                throw new TrichologyValidationException("Valor fora da faixa plausível para " + key + ".");
            }
        });
        return new AssessmentImage(id, companyId, assessmentId, imageId, regionCode, magnification, qualityStatus,
                aiStatus, aiImageQuality, aiFindings, aiEstimates, aiNotes,
                accepted == null ? List.of() : List.copyOf(accepted), Map.copyOf(m), include, userId, now,
                createdAt, now);
    }
}
