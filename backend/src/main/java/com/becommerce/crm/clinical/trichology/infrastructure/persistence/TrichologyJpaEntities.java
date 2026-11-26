package com.becommerce.crm.clinical.trichology.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidades JPA do módulo (V151–V153). Campos de pacote: só o adapter deste
 * pacote as manipula; colunas JSONB trafegam como texto JSON convertido pelo adapter.
 */
final class TrichologyJpaEntities {

    private TrichologyJpaEntities() {
    }

    @Entity(name = "TrichologyAssessmentEntity")
    @Table(name = "trichology_assessments")
    static class AssessmentEntity {
        @Id UUID id;
        @Column(name = "company_id", nullable = false) UUID companyId;
        @Column(name = "contact_id", nullable = false) UUID contactId;
        @Column(name = "professional_id", nullable = false) UUID professionalId;
        @Column(name = "protocol_id") UUID protocolId;
        @Column(nullable = false) String status;
        @Column(name = "consent_at") Instant consentAt;
        @Column(name = "consent_by") UUID consentBy;
        @Column(name = "chief_complaint") String chiefComplaint;
        @JdbcTypeCode(SqlTypes.JSON) @Column(name = "quiz_answers", columnDefinition = "jsonb") String quizAnswers;
        @Column(name = "quiz_completed_at") Instant quizCompletedAt;
        @Column(name = "quiz_form_token_hash") String quizFormTokenHash;
        @Column(name = "quiz_form_expires_at") Instant quizFormExpiresAt;
        @Column(name = "clinical_notes") String clinicalNotes;
        @JdbcTypeCode(SqlTypes.JSON) @Column(name = "pull_test", columnDefinition = "jsonb") String pullTest;
        @Column(name = "trichoscopy_performed", nullable = false) boolean trichoscopyPerformed;
        @JdbcTypeCode(SqlTypes.JSON) @Column(name = "skipped_regions", columnDefinition = "jsonb", nullable = false)
        String skippedRegions;
        @JdbcTypeCode(SqlTypes.JSON) @Column(name = "requested_exams", columnDefinition = "jsonb") String requestedExams;
        @JdbcTypeCode(SqlTypes.JSON) @Column(name = "lab_results", columnDefinition = "jsonb") String labResults;
        @Column(name = "lab_collected_on") LocalDate labCollectedOn;
        @JdbcTypeCode(SqlTypes.JSON) @Column(name = "rps_summary", columnDefinition = "jsonb") String rpsSummary;
        @JdbcTypeCode(SqlTypes.JSON) @Column(name = "ai_draft_payload", columnDefinition = "jsonb") String aiDraftPayload;
        @JdbcTypeCode(SqlTypes.JSON) @Column(name = "final_report_payload", columnDefinition = "jsonb")
        String finalReportPayload;
        @Column(name = "report_version", nullable = false) int reportVersion;
        @Column(name = "supersedes_id") UUID supersedesId;
        @Column(name = "patient_document_id") UUID patientDocumentId;
        @Column(name = "verification_code") String verificationCode;
        @Column(name = "pdf_sha256") String pdfSha256;
        @Column(name = "pdf_storage_object_id") UUID pdfStorageObjectId;
        @Column(name = "professional_name") String professionalName;
        @Column(name = "professional_registry") String professionalRegistry;
        @Column(name = "reviewed_by") UUID reviewedBy;
        @Column(name = "reviewed_at") Instant reviewedAt;
        @Column(name = "published_at") Instant publishedAt;
        @Column(name = "cancelled_at") Instant cancelledAt;
        @Column(name = "cancel_reason") String cancelReason;
        @Column(name = "created_by", nullable = false) UUID createdBy;
        @Column(name = "created_at", nullable = false) Instant createdAt;
        @Column(name = "updated_at", nullable = false) Instant updatedAt;
    }

    @Entity(name = "TrichologyAssessmentImageEntity")
    @Table(name = "trichology_assessment_images")
    static class AssessmentImageEntity {
        @Id UUID id;
        @Column(name = "company_id", nullable = false) UUID companyId;
        @Column(name = "assessment_id", nullable = false) UUID assessmentId;
        @Column(name = "image_id", nullable = false) UUID imageId;
        @Column(name = "region_code", nullable = false) String regionCode;
        @Column(name = "magnification") String magnification;
        @Column(name = "quality_status", nullable = false) String qualityStatus;
        @Column(name = "ai_status", nullable = false) String aiStatus;
        @Column(name = "ai_image_quality") String aiImageQuality;
        @JdbcTypeCode(SqlTypes.JSON) @Column(name = "ai_findings", columnDefinition = "jsonb") String aiFindings;
        @JdbcTypeCode(SqlTypes.JSON) @Column(name = "ai_estimates", columnDefinition = "jsonb") String aiEstimates;
        @Column(name = "ai_notes") String aiNotes;
        @JdbcTypeCode(SqlTypes.JSON) @Column(name = "accepted_findings", columnDefinition = "jsonb", nullable = false)
        String acceptedFindings;
        @JdbcTypeCode(SqlTypes.JSON) @Column(name = "confirmed_measures", columnDefinition = "jsonb", nullable = false)
        String confirmedMeasures;
        @Column(name = "include_in_report", nullable = false) boolean includeInReport;
        @Column(name = "reviewed_by") UUID reviewedBy;
        @Column(name = "reviewed_at") Instant reviewedAt;
        @Column(name = "created_at", nullable = false) Instant createdAt;
        @Column(name = "updated_at", nullable = false) Instant updatedAt;
    }

    @Entity(name = "TrichologyProtocolEntity")
    @Table(name = "trichology_capture_protocols")
    static class ProtocolEntity {
        @Id UUID id;
        @Column(name = "company_id", nullable = false) UUID companyId;
        @Column(nullable = false) String name;
        @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb", nullable = false) String regions;
        @Column(name = "is_default", nullable = false) boolean isDefault;
        @Column(nullable = false) boolean active;
        @Column(name = "created_at", nullable = false) Instant createdAt;
        @Column(name = "updated_at", nullable = false) Instant updatedAt;
    }

    @Entity(name = "TrichologyFindingEntity")
    @Table(name = "trichology_finding_catalog")
    static class FindingEntity {
        @Id UUID id;
        @Column(name = "company_id", nullable = false) UUID companyId;
        @Column(nullable = false) String code;
        @Column(nullable = false) String label;
        @Column String description;
        @Column(nullable = false) boolean active;
        @Column(name = "created_at", nullable = false) Instant createdAt;
        @Column(name = "updated_at", nullable = false) Instant updatedAt;
    }

    @Entity(name = "TrichologyMarkerEntity")
    @Table(name = "trichology_markers")
    static class MarkerEntity {
        @Id UUID id;
        @Column(name = "company_id", nullable = false) UUID companyId;
        @Column(nullable = false) String code;
        @Column(nullable = false) String label;
        @Column String unit;
        @Column(nullable = false) String system;
        @Column String sex;
        @Column(name = "reference_min") BigDecimal referenceMin;
        @Column(name = "reference_max") BigDecimal referenceMax;
        @Column(name = "target_quartile") Integer targetQuartile;
        @Column(nullable = false) boolean active;
        @Column(name = "created_at", nullable = false) Instant createdAt;
        @Column(name = "updated_at", nullable = false) Instant updatedAt;
    }

    @Entity(name = "TrichologySettingsEntity")
    @Table(name = "trichology_settings")
    static class SettingsEntity {
        @Id @Column(name = "company_id") UUID companyId;
        @Column(name = "min_confidence", nullable = false) BigDecimal minConfidence;
        @Column(name = "report_horizons_months", nullable = false) String reportHorizonsMonths;
        @JdbcTypeCode(SqlTypes.JSON) @Column(name = "exam_rules", columnDefinition = "jsonb", nullable = false)
        String examRules;
        @Column(name = "created_at", nullable = false) Instant createdAt;
        @Column(name = "updated_at", nullable = false) Instant updatedAt;
    }

    @Entity(name = "TrichologyAiRunEntity")
    @Table(name = "trichology_ai_runs")
    static class AiRunEntity {
        @Id UUID id;
        @Column(name = "company_id", nullable = false) UUID companyId;
        @Column(name = "assessment_id", nullable = false) UUID assessmentId;
        @Column(name = "assessment_image_id") UUID assessmentImageId;
        @Column(nullable = false) String stage;
        @Column String model;
        @Column(name = "prompt_version", nullable = false) String promptVersion;
        @Column(name = "image_sha256") String imageSha256;
        @Column(name = "input_tokens") Integer inputTokens;
        @Column(name = "output_tokens") Integer outputTokens;
        @Column(nullable = false) String status;
        @Column(name = "error_code") String errorCode;
        @Column(name = "latency_ms") Integer latencyMs;
        @Column(name = "created_by") UUID createdBy;
        @Column(name = "created_at", nullable = false) Instant createdAt;
    }
}
