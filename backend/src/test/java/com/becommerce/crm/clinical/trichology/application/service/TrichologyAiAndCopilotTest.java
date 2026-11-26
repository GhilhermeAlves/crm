package com.becommerce.crm.clinical.trichology.application.service;

import com.becommerce.crm.clinical.trichology.application.dto.TrichologyDtos.Hypothesis;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.PullTest;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.PullTestSite;
import com.becommerce.crm.clinical.trichology.domain.AssessmentImage;
import com.becommerce.crm.clinical.trichology.domain.Finding;
import com.becommerce.crm.clinical.trichology.domain.TrichologyAssessment;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Contrato da resposta da IA de visão (guardrail no parse) e regras do copiloto. */
class TrichologyAiAndCopilotTest {

    private static final UUID COMPANY = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-25T12:00:00Z");
    private static final Set<String> CATALOG = Set.of("MINIATURIZATION", "YELLOW_DOTS");
    private static final Map<String, String> LABELS = Map.of("MINIATURIZATION", "Miniaturização de fios",
            "YELLOW_DOTS", "Pontos amarelos");

    private final ImageAnalysisService analysis = new ImageAnalysisService(null, null, null, null, null, null,
            null, null, new ObjectMapper(), null, null);

    @Test
    void parseKeepsOnlyCatalogFindingsAndPlausibleEstimates() {
        String json = """
                {"imageQuality":"OK",
                 "findings":[{"code":"MINIATURIZATION","severity":"MODERATE","confidence":0.82,"evidence":"fios finos"},
                             {"code":"INVENTED","severity":"MILD","confidence":0.9,"evidence":"x"}],
                 "estimates":{"hairDensityPerCm2":180,"thinHairPercent":250},
                 "notes":"ok"}""";
        ImageAnalysisService.Parsed parsed = analysis.parse(json, CATALOG, LABELS);

        assertThat(parsed.findings()).extracting(Finding::code).containsExactly("MINIATURIZATION");
        assertThat(parsed.findings().get(0).label()).isEqualTo("Miniaturização de fios");
        assertThat(parsed.estimates()).containsOnlyKeys("hairDensityPerCm2");
        assertThat(parsed.violations()).hasSize(2);
    }

    @Test
    void lowQualityImageYieldsNoFindings() {
        String json = """
                {"imageQuality":"BLURRED","findings":[{"code":"YELLOW_DOTS","confidence":0.7}],
                 "estimates":{"hairDensityPerCm2":150}}""";
        ImageAnalysisService.Parsed parsed = analysis.parse(json, CATALOG, LABELS);

        assertThat(parsed.quality()).isEqualTo("BLURRED");
        assertThat(parsed.findings()).isEmpty();
        assertThat(parsed.estimates()).isEmpty();
    }

    @Test
    void invalidJsonIsReportedWithoutFindings() {
        ImageAnalysisService.Parsed parsed = analysis.parse("não é json", CATALOG, LABELS);
        assertThat(parsed.findings()).isEmpty();
        assertThat(parsed.violations()).isNotEmpty();
    }

    @Test
    void hypothesesUseOnlyAcceptedFindingsAndCiteSupport() {
        TrichologyAssessment a = TrichologyAssessment.open(COMPANY, UUID.randomUUID(), USER, UUID.randomUUID(),
                true, null, USER, NOW);
        a.completeQuiz(Map.of("pattern", "THINNING_TOP", "family_history", true), null, NOW);
        AssessmentImage proposedOnly = AssessmentImage.link(COMPANY, a.getId(), UUID.randomUUID(), "VERTEX", "20x",
                "OK", true, NOW).withAnalysis("OK", List.of(new Finding("MINIATURIZATION", "Miniaturização", "MILD",
                0.9, null, Finding.SOURCE_AI)), Map.of(), null, NOW);

        List<Hypothesis> fromQuizOnly = ExamCopilotService.hypotheses(a, List.of(proposedOnly));
        assertThat(fromQuizOnly).extracting(Hypothesis::code).containsExactly("ANDROGENETIC_PATTERN");
        assertThat(fromQuizOnly.get(0).support()).noneMatch(s -> s.startsWith("Achado"));

        AssessmentImage accepted = proposedOnly.reviewed(List.of(new Finding("MINIATURIZATION", "Miniaturização",
                "MILD", null, null, Finding.SOURCE_AI)), Map.of("thinHairPercent", new BigDecimal("35")), true, USER,
                NOW);
        List<Hypothesis> withFinding = ExamCopilotService.hypotheses(a, List.of(accepted));
        assertThat(withFinding.get(0).support()).contains("Achado: Miniaturização");
    }

    @Test
    void positivePullTestSupportsEffluvium() {
        TrichologyAssessment a = TrichologyAssessment.open(COMPANY, UUID.randomUUID(), USER, UUID.randomUUID(),
                true, null, USER, NOW);
        a.completeQuiz(Map.of("pattern", "DIFFUSE_SHEDDING"), null, NOW);
        a.saveClinical(new PullTest(List.of(new PullTestSite("VERTEX", 9)), null), null, NOW);

        assertThat(ExamCopilotService.hypotheses(a, List.of())).extracting(Hypothesis::code)
                .contains("TELOGEN_EFFLUVIUM");
    }

    @Test
    void measuresAverageByRegion() {
        AssessmentImage one = AssessmentImage.link(COMPANY, UUID.randomUUID(), UUID.randomUUID(), "VERTEX", null,
                "OK", false, NOW).reviewed(List.of(), Map.of("thinHairPercent", new BigDecimal("30")), true, USER,
                NOW);
        AssessmentImage two = AssessmentImage.link(COMPANY, UUID.randomUUID(), UUID.randomUUID(), "VERTEX", null,
                "OK", false, NOW).reviewed(List.of(), Map.of("thinHairPercent", new BigDecimal("40")), true, USER,
                NOW);
        assertThat(ExamCopilotService.measures(List.of(one, two)))
                .singleElement()
                .satisfies(m -> assertThat(m.value()).isEqualByComparingTo("35"));
    }
}
