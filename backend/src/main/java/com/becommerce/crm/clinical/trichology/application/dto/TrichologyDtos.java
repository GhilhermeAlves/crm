package com.becommerce.crm.clinical.trichology.application.dto;

import com.becommerce.crm.clinical.trichology.domain.AssessmentData.LabResult;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.PullTest;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.ReportPayload;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.ReportSection;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.RpsSummary;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.SkippedRegion;
import com.becommerce.crm.clinical.trichology.domain.CaptureProtocol;
import com.becommerce.crm.clinical.trichology.domain.CatalogItems.ExamRule;
import com.becommerce.crm.clinical.trichology.domain.CatalogItems.FindingCatalogItem;
import com.becommerce.crm.clinical.trichology.domain.CatalogItems.Marker;
import com.becommerce.crm.clinical.trichology.domain.Finding;
import com.becommerce.crm.clinical.trichology.domain.QuizEngine;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Requisições e respostas da API de tricologia (doc v1.2, seção 7). */
public final class TrichologyDtos {

    private TrichologyDtos() {
    }

    // ------------------------------------------------------------ requisições

    public record OpenAssessmentRequest(UUID protocolId, boolean consentGiven, String chiefComplaint) {
    }

    public record CancelRequest(String reason) {
    }

    public record QuizRequest(Map<String, Object> answers, boolean complete) {
    }

    public record ClinicalRequest(PullTest pullTest, String notes) {
    }

    public record FinishImagingRequest(List<SkippedRegion> skippedRegions) {
    }

    public record LinkImageRequest(UUID imageId, String regionCode, String magnification, String qualityStatus) {
    }

    public record ReviewImageRequest(List<Finding> acceptedFindings, Map<String, BigDecimal> confirmedMeasures,
                                     Boolean includeInReport) {
    }

    public record ExamsRequest(List<String> markerCodes) {
    }

    public record LabsRequest(List<LabResult> results, LocalDate collectedOn) {
    }

    public record ReportUpdateRequest(List<ReportSection> sections) {
    }

    public record ApproveRequest(String professionalName, String professionalRegistry) {
    }

    public record SettingsUpdateRequest(BigDecimal minConfidence, List<Integer> reportHorizonsMonths,
                                        List<ExamRule> examRules) {
    }

    public record ProtocolRequest(String name, List<CaptureProtocol.Region> regions, boolean isDefault,
                                  boolean active) {
    }

    public record FindingRequest(String code, String label, String description, boolean active) {
    }

    public record MarkerRequest(String code, String label, String unit, String system, String sex,
                                BigDecimal referenceMin, BigDecimal referenceMax, Integer targetQuartile,
                                boolean active) {
    }

    public record PublicQuizSubmit(Map<String, Object> answers, String website) {
    }

    // ------------------------------------------------------------ respostas

    public record AssessmentSummary(UUID id, String status, int reportVersion, boolean trichoscopyPerformed,
                                    UUID supersedesId, String professionalName, Instant createdAt,
                                    Instant publishedAt, Instant cancelledAt) {
    }

    /** Achado da IA com a marca de "possível" (confiança abaixo do limite). */
    public record FindingView(String code, String label, String severity, Double confidence, String evidence,
                              String source, boolean possible) {
    }

    public record ImageView(UUID id, UUID imageId, String regionCode, String regionLabel, String magnification,
                            String qualityStatus, String aiStatus, String aiImageQuality, List<FindingView> aiFindings,
                            Map<String, BigDecimal> aiEstimates, String aiNotes, List<Finding> acceptedFindings,
                            Map<String, BigDecimal> confirmedMeasures, boolean includeInReport, Instant reviewedAt,
                            Instant takenAt, String downloadUrl) {
    }

    public record RegionProgress(String code, String label, int minImages, int images, boolean skipped,
                                 String skipReason) {
    }

    public record AssessmentDetail(UUID id, UUID contactId, String status, int reportVersion, UUID supersedesId,
                                   CaptureProtocol protocol, List<RegionProgress> regions, Instant consentAt,
                                   String chiefComplaint, Map<String, Object> quizAnswers, Instant quizCompletedAt,
                                   Instant quizLinkExpiresAt, String clinicalNotes, PullTest pullTest,
                                   boolean pullTestPositive, boolean trichoscopyPerformed, List<ImageView> images,
                                   List<String> requestedExams, List<LabResult> labResults, LocalDate labCollectedOn,
                                   RpsSummary rpsSummary, ReportPayload report, String verificationCode,
                                   String professionalName, String professionalRegistry, Instant reviewedAt,
                                   Instant publishedAt, Instant cancelledAt, String cancelReason, Instant createdAt,
                                   Instant updatedAt) {
    }

    public record QuizLinkResponse(String url, Instant expiresAt) {
    }

    public record QuizDefinition(String version, List<QuizEngine.Question> questions) {
    }

    public record Hypothesis(String code, String label, List<String> support) {
    }

    public record SuggestedExam(String markerCode, String label, String reason) {
    }

    public record MeasureDelta(String regionCode, String measure, BigDecimal previous, BigDecimal current) {
    }

    public record Evolution(UUID previousAssessmentId, Instant previousPublishedAt, List<MeasureDelta> deltas,
                            Integer previousRps, Integer currentRps) {
    }

    public record CopilotView(List<String> pending, List<Hypothesis> hypotheses, List<SuggestedExam> suggestedExams,
                              Evolution evolution, String disclaimer) {
    }

    public record SettingsView(BigDecimal minConfidence, List<Integer> reportHorizonsMonths, List<ExamRule> examRules,
                               List<CaptureProtocol> protocols, List<FindingCatalogItem> findings,
                               List<Marker> markers) {
    }

    public record PublicQuizView(String clinicName, String patientFirstName, QuizDefinition quiz) {
    }

    public record VerifyView(boolean valid, Instant publishedAt, String professionalName, String professionalRegistry,
                             String clinicName, Integer reportVersion, String pdfSha256, boolean superseded) {
    }

    public record ReportPdf(String fileName, byte[] content) {
    }
}
