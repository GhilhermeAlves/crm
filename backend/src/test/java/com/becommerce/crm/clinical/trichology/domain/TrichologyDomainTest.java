package com.becommerce.crm.clinical.trichology.domain;

import com.becommerce.crm.clinical.trichology.domain.AssessmentData.LabResult;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.PullTest;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.PullTestSite;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.ReportPayload;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.ReportSection;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.RpsSummary;
import com.becommerce.crm.clinical.trichology.domain.CatalogItems.Marker;
import com.becommerce.crm.clinical.trichology.domain.exception.TrichologyStateException;
import com.becommerce.crm.clinical.trichology.domain.exception.TrichologyValidationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrichologyDomainTest {

    private static final UUID COMPANY = UUID.randomUUID();
    private static final UUID CONTACT = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-25T12:00:00Z");

    @Nested
    class RpsCalculatorTest {

        private final List<Marker> markers = List.of(
                marker("FERRITIN", "FERRO", "F", "15", "150", 3),
                marker("FERRITIN", "FERRO", "M", "30", "400", 3),
                marker("TSH", "TIREOIDE", null, "0.4", "4.5", 2));

        private Marker marker(String code, String system, String sex, String min, String max, Integer target) {
            return new Marker(UUID.randomUUID(), COMPANY, code, code, "u", system, sex, new BigDecimal(min),
                    new BigDecimal(max), target, true);
        }

        @Test
        void scoresByQuartileAndAveragesBySystem() {
            RpsSummary rps = RpsCalculator.calculate(List.of(
                    new LabResult("FERRITIN", new BigDecimal("100"), "ng/mL"),
                    new LabResult("TSH", new BigDecimal("1.0"), "µUI/mL")), markers, "F");

            assertThat(rps.markers()).hasSize(2);
            // ferritina 100 em 15–150: fração 0,63 → Q3 = meta → 100
            assertThat(rps.markers().get(0).quartile()).isEqualTo(3);
            assertThat(rps.markers().get(0).score()).isEqualTo(100);
            // TSH 1,0 em 0,4–4,5: fração 0,15 → Q1, meta Q2 → 75
            assertThat(rps.markers().get(1).score()).isEqualTo(75);
            assertThat(rps.overallScore()).isEqualTo(88);
        }

        @Test
        void outOfRangeScoresZeroAndUsesSexSpecificRange() {
            RpsSummary rps = RpsCalculator.calculate(List.of(
                    new LabResult("FERRITIN", new BigDecimal("20"), "ng/mL")), markers, "M");
            assertThat(rps.markers().get(0).position()).isEqualTo("LOW");
            assertThat(rps.markers().get(0).score()).isZero();
        }

        @Test
        void unknownMarkerIsReportedNotScored() {
            RpsSummary rps = RpsCalculator.calculate(List.of(
                    new LabResult("XYZ", BigDecimal.ONE, null)), markers, "F");
            assertThat(rps.markers()).isEmpty();
            assertThat(rps.unknownMarkers()).containsExactly("XYZ");
        }
    }

    @Nested
    class GuardrailTest {

        @Test
        void rejectsFindingsOutsideCatalogAndOnBadImages() {
            Finding outside = new Finding("NOT_IN_CATALOG", null, "MILD", 0.9, null, Finding.SOURCE_AI);
            Finding ok = new Finding("MINIATURIZATION", null, "MILD", 0.9, null, Finding.SOURCE_AI);
            assertThat(GuardrailValidator.validateFindings(List.of(outside), Set.of("MINIATURIZATION"), "OK"))
                    .hasSize(1);
            assertThat(GuardrailValidator.validateFindings(List.of(ok), Set.of("MINIATURIZATION"), "BLURRED"))
                    .isNotEmpty();
            assertThat(GuardrailValidator.validateFindings(List.of(ok), Set.of("MINIATURIZATION"), "OK")).isEmpty();
        }

        @Test
        void blocksDiagnosisLanguageOnlyInAiText() {
            ReportSection ai = new ReportSection("analysis", "Análise", "O diagnóstico é alopecia.",
                    ReportSection.AI);
            ReportSection edited = new ReportSection("analysis", "Análise", "O diagnóstico é alopecia.",
                    ReportSection.EDITED);
            assertThat(GuardrailValidator.validateReport(List.of(ai), true)).hasSize(1);
            assertThat(GuardrailValidator.validateReport(List.of(edited), true)).isEmpty();
        }

        @Test
        void blocksTrichoscopyClaimWhenNotPerformed() {
            ReportSection s = new ReportSection("analysis", "Análise", "Na tricoscopia vimos miniaturização.",
                    ReportSection.EDITED);
            assertThat(GuardrailValidator.validateReport(List.of(s), false)).hasSize(1);
            assertThat(GuardrailValidator.validateReport(List.of(s), true)).isEmpty();
        }

        @Test
        void implausibleEstimatesAreRejected() {
            assertThat(GuardrailValidator.plausible("thinHairPercent", new BigDecimal("140"))).isFalse();
            assertThat(GuardrailValidator.plausible("thinHairPercent", new BigDecimal("40"))).isTrue();
            assertThat(GuardrailValidator.plausible("unknown", BigDecimal.ONE)).isFalse();
        }
    }

    @Nested
    class VerificationCodeTest {

        @Test
        void generatesWellFormedUnambiguousCodes() {
            for (int i = 0; i < 200; i++) {
                String code = VerificationCode.generate();
                assertThat(VerificationCode.isWellFormed(code)).isTrue();
                assertThat(code).doesNotContain("0", "O", "1", "I", "L");
            }
        }

        @Test
        void normalizesUserInput() {
            assertThat(VerificationCode.normalize("abcd-efgh-jkmn")).isEqualTo("ABCDEFGHJKMN");
            assertThat(VerificationCode.isWellFormed("ABC")).isFalse();
        }
    }

    @Nested
    class AssessmentLifecycleTest {

        private TrichologyAssessment open() {
            return TrichologyAssessment.open(COMPANY, CONTACT, USER, UUID.randomUUID(), true, "Queda", USER, NOW);
        }

        private ReportPayload report(List<String> violations) {
            return new ReportPayload(List.of(new ReportSection("analysis", "Análise", "Texto", ReportSection.EDITED)),
                    Map.of(), violations);
        }

        @Test
        void requiresConsent() {
            assertThatThrownBy(() -> TrichologyAssessment.open(COMPANY, CONTACT, USER, null, false, null, USER, NOW))
                    .isInstanceOf(TrichologyValidationException.class);
        }

        @Test
        void statusKeepsTheMostAdvancedCollectionStep() {
            TrichologyAssessment a = open();
            a.saveClinical(new PullTest(List.of(new PullTestSite("VERTEX", 8)), null), "obs", NOW);
            assertThat(a.getStatus()).isEqualTo(AssessmentStatus.CLINICAL_DONE);
            a.imageLinked(NOW);
            assertThat(a.getStatus()).isEqualTo(AssessmentStatus.CLINICAL_DONE);
            assertThat(a.isTrichoscopyPerformed()).isTrue();
            assertThat(a.state().pullTest().positive()).isTrue();
        }

        @Test
        void newCollectionAfterDraftReturnsToCollection() {
            TrichologyAssessment a = open();
            a.draftReady(report(List.of()), false, NOW);
            assertThat(a.getStatus()).isEqualTo(AssessmentStatus.AI_DRAFT_READY);
            a.imageLinked(NOW);
            assertThat(a.getStatus()).isEqualTo(AssessmentStatus.IMAGING_IN_PROGRESS);
        }

        @Test
        void publishRequiresCleanReview() {
            TrichologyAssessment a = open();
            a.draftReady(report(List.of()), false, NOW);
            assertThatThrownBy(() -> a.publish("ABCDEFGHJKMN", "sha", UUID.randomUUID(), UUID.randomUUID(),
                    "Dra. Ana", null, NOW)).isInstanceOf(TrichologyStateException.class);

            a.review(report(List.of("violação")), USER, NOW);
            assertThat(a.getStatus()).isEqualTo(AssessmentStatus.GUARDRAIL_BLOCKED);

            a.review(report(List.of()), USER, NOW);
            a.publish("ABCDEFGHJKMN", "sha", UUID.randomUUID(), UUID.randomUUID(), "Dra. Ana", "CRM 1", NOW);
            assertThat(a.getStatus()).isEqualTo(AssessmentStatus.PUBLISHED);
        }

        @Test
        void publishedIsFrozenAndNewVersionPointsToIt() {
            TrichologyAssessment a = open();
            a.review(report(List.of()), USER, NOW);
            a.publish("ABCDEFGHJKMN", "sha", UUID.randomUUID(), UUID.randomUUID(), "Dra. Ana", null, NOW);

            assertThatThrownBy(() -> a.saveClinical(null, "x", NOW)).isInstanceOf(TrichologyStateException.class);
            TrichologyAssessment next = a.newVersion(USER, NOW);
            assertThat(next.state().supersedesId()).isEqualTo(a.getId());
            assertThat(next.state().reportVersion()).isEqualTo(2);
            assertThat(next.getStatus()).isEqualTo(AssessmentStatus.AI_DRAFT_READY);
            assertThat(next.state().verificationCode()).isNull();
        }

        @Test
        void skippingARegionNeedsAReason() {
            TrichologyAssessment a = open();
            assertThatThrownBy(() -> a.finishImaging(List.of(new AssessmentData.SkippedRegion("VERTEX", " ")), NOW))
                    .isInstanceOf(TrichologyValidationException.class);
        }
    }

    @Nested
    class AssessmentImageTest {

        @Test
        void confirmedMeasuresMustBePlausible() {
            AssessmentImage image = AssessmentImage.link(COMPANY, UUID.randomUUID(), UUID.randomUUID(), "VERTEX",
                    "20x", "OK", true, NOW);
            assertThatThrownBy(() -> image.reviewed(List.of(), Map.of("thinHairPercent", new BigDecimal("120")),
                    true, USER, NOW)).isInstanceOf(TrichologyValidationException.class);
            assertThat(image.reviewed(List.of(), Map.of("thinHairPercent", new BigDecimal("30")), true, USER, NOW)
                    .confirmedMeasures()).containsKey("thinHairPercent");
        }

        @Test
        void aiDisabledLinksAsSkipped() {
            AssessmentImage image = AssessmentImage.link(COMPANY, UUID.randomUUID(), UUID.randomUUID(), "VERTEX",
                    null, null, false, NOW);
            assertThat(image.aiStatus()).isEqualTo(AssessmentImage.AI_SKIPPED);
            assertThat(image.qualityStatus()).isEqualTo("UNCHECKED");
        }
    }
}
