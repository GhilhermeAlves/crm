package com.becommerce.crm.clinical.trichology.domain;

import com.becommerce.crm.clinical.trichology.domain.AssessmentData.LabResult;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.PullTest;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.ReportPayload;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.RpsSummary;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.SkippedRegion;
import com.becommerce.crm.clinical.trichology.domain.exception.TrichologyStateException;
import com.becommerce.crm.clinical.trichology.domain.exception.TrichologyValidationException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Exame tricológico (doc v1.2, seção 2). Reúne quiz, captura por região, exame
 * clínico, laboratório e relatório. Depois de PUBLISHED nada muda: correção é
 * uma nova versão ({@link #newVersion}).
 */
public class TrichologyAssessment {

    /** Todos os campos persistidos; usado só pelo adapter de persistência. */
    public record State(UUID id, UUID companyId, UUID contactId, UUID professionalId, UUID protocolId,
                        AssessmentStatus status, Instant consentAt, UUID consentBy, String chiefComplaint,
                        Map<String, Object> quizAnswers, Instant quizCompletedAt, String quizFormTokenHash,
                        Instant quizFormExpiresAt, String clinicalNotes, PullTest pullTest,
                        boolean trichoscopyPerformed, List<SkippedRegion> skippedRegions,
                        List<String> requestedExams, List<LabResult> labResults, LocalDate labCollectedOn,
                        RpsSummary rpsSummary, ReportPayload aiDraftPayload, ReportPayload finalReportPayload,
                        int reportVersion, UUID supersedesId, UUID patientDocumentId, String verificationCode,
                        String pdfSha256, UUID pdfStorageObjectId, String professionalName,
                        String professionalRegistry, UUID reviewedBy, Instant reviewedAt, Instant publishedAt,
                        Instant cancelledAt, String cancelReason, UUID createdBy, Instant createdAt,
                        Instant updatedAt) {
    }

    private State s;

    private TrichologyAssessment(State state) {
        this.s = state;
    }

    public static TrichologyAssessment reconstitute(State state) {
        return new TrichologyAssessment(state);
    }

    public State state() {
        return s;
    }

    public static TrichologyAssessment open(UUID companyId, UUID contactId, UUID professionalId, UUID protocolId,
                                            boolean consentGiven, String chiefComplaint, UUID userId, Instant now) {
        if (!consentGiven) {
            throw new TrichologyValidationException(
                    "Registre o consentimento do paciente para imagens e dados de saúde (LGPD art. 11).");
        }
        return new TrichologyAssessment(new State(UUID.randomUUID(), companyId, contactId, professionalId,
                protocolId, AssessmentStatus.DRAFT, now, userId, clean(chiefComplaint, 2000, "Queixa principal"),
                null, null, null, null, null, null, false, List.of(), null, null, null, null, null, null, 1, null,
                null, null, null, null, null, null, null, null, null, null, null, userId, now, now));
    }

    /**
     * Nova versão de um relatório publicado: copia as coletas e volta para
     * revisão; o relatório anterior continua válido até a nova ser publicada.
     */
    public TrichologyAssessment newVersion(UUID userId, Instant now) {
        if (s.status() != AssessmentStatus.PUBLISHED) {
            throw new TrichologyStateException("Só um relatório publicado pode ganhar nova versão.");
        }
        return new TrichologyAssessment(new State(UUID.randomUUID(), s.companyId(), s.contactId(),
                s.professionalId(), s.protocolId(), AssessmentStatus.AI_DRAFT_READY, s.consentAt(), s.consentBy(),
                s.chiefComplaint(), s.quizAnswers(), s.quizCompletedAt(), null, null, s.clinicalNotes(),
                s.pullTest(), s.trichoscopyPerformed(), s.skippedRegions(), s.requestedExams(), s.labResults(),
                s.labCollectedOn(), s.rpsSummary(), s.aiDraftPayload(), s.finalReportPayload(),
                s.reportVersion() + 1, s.id(), null, null, null, null, s.professionalName(),
                s.professionalRegistry(), null, null, null, null, null, userId, now, now));
    }

    // ---------------------------------------------------------------- coletas

    public void setQuizLink(String tokenHash, Instant expiresAt, Instant now) {
        requireEditable();
        s = with(b -> {
            b.quizFormTokenHash = tokenHash;
            b.quizFormExpiresAt = expiresAt;
        }, now);
    }

    public void completeQuiz(Map<String, Object> answers, String chiefComplaint, Instant now) {
        requireEditable();
        s = with(b -> {
            b.quizAnswers = answers;
            b.quizCompletedAt = now;
            b.quizFormTokenHash = null;
            b.quizFormExpiresAt = null;
            if (chiefComplaint != null && !chiefComplaint.isBlank()) {
                b.chiefComplaint = clean(chiefComplaint, 2000, "Queixa principal");
            }
        }, now);
        advance(AssessmentStatus.QUIZ_COMPLETED, now);
    }

    public void imageLinked(Instant now) {
        requireEditable();
        s = with(b -> b.trichoscopyPerformed = true, now);
        advance(AssessmentStatus.IMAGING_IN_PROGRESS, now);
    }

    public void finishImaging(List<SkippedRegion> skipped, Instant now) {
        requireEditable();
        List<SkippedRegion> list = skipped == null ? List.of() : List.copyOf(skipped);
        for (SkippedRegion r : list) {
            if (r.reason() == null || r.reason().isBlank()) {
                throw new TrichologyValidationException("Informe o motivo para pular a região " + r.regionCode() + ".");
            }
        }
        s = with(b -> b.skippedRegions = list, now);
        advance(AssessmentStatus.IMAGING_DONE, now);
    }

    public void saveClinical(PullTest pullTest, String notes, Instant now) {
        requireEditable();
        if (pullTest != null && pullTest.sites() != null) {
            pullTest.sites().forEach(site -> {
                if (site.hairsPulled() < 0 || site.hairsPulled() > 200) {
                    throw new TrichologyValidationException("Quantidade de fios do pull test inválida.");
                }
            });
        }
        s = with(b -> {
            b.pullTest = pullTest;
            b.clinicalNotes = clean(notes, 10000, "Observações");
        }, now);
        advance(AssessmentStatus.CLINICAL_DONE, now);
    }

    public void requestExams(List<String> markerCodes, Instant now) {
        requireEditable();
        s = with(b -> b.requestedExams = markerCodes == null ? List.of() : List.copyOf(markerCodes), now);
        advance(AssessmentStatus.EXAMS_REQUESTED, now);
    }

    public void loadLabs(List<LabResult> results, LocalDate collectedOn, RpsSummary rps, Instant now) {
        requireEditable();
        s = with(b -> {
            b.labResults = results == null ? List.of() : List.copyOf(results);
            b.labCollectedOn = collectedOn;
            b.rpsSummary = rps;
        }, now);
        advance(AssessmentStatus.LABS_LOADED, now);
    }

    // -------------------------------------------------------------- relatório

    public void draftReady(ReportPayload draft, boolean blocked, Instant now) {
        requireEditable();
        s = with(b -> {
            b.aiDraftPayload = draft;
            b.finalReportPayload = draft;
            b.status = blocked ? AssessmentStatus.GUARDRAIL_BLOCKED : AssessmentStatus.AI_DRAFT_READY;
        }, now);
    }

    /** Texto revisado pelo profissional; o guardrail precisa estar limpo para aprovar. */
    public void review(ReportPayload edited, UUID userId, Instant now) {
        requireEditable();
        if (edited == null || edited.sections() == null || edited.sections().isEmpty()) {
            throw new TrichologyValidationException("O relatório precisa de pelo menos uma seção.");
        }
        boolean blocked = edited.guardrailViolations() != null && !edited.guardrailViolations().isEmpty();
        s = with(b -> {
            b.finalReportPayload = edited;
            b.status = blocked ? AssessmentStatus.GUARDRAIL_BLOCKED : AssessmentStatus.REVIEWED;
            b.reviewedBy = blocked ? null : userId;
            b.reviewedAt = blocked ? null : now;
        }, now);
    }

    public void publish(String verificationCode, String pdfSha256, UUID pdfStorageObjectId, UUID documentId,
                        String professionalName, String professionalRegistry, Instant now) {
        if (s.status() != AssessmentStatus.REVIEWED) {
            throw new TrichologyStateException("Revise e salve o relatório sem pendências antes de publicar.");
        }
        if (professionalName == null || professionalName.isBlank()) {
            throw new TrichologyValidationException("Informe o nome do profissional que assina.");
        }
        s = with(b -> {
            b.status = AssessmentStatus.PUBLISHED;
            b.verificationCode = verificationCode;
            b.pdfSha256 = pdfSha256;
            b.pdfStorageObjectId = pdfStorageObjectId;
            b.patientDocumentId = documentId;
            b.professionalName = professionalName.trim();
            b.professionalRegistry = professionalRegistry == null || professionalRegistry.isBlank()
                    ? null : professionalRegistry.trim();
            b.publishedAt = now;
        }, now);
    }

    public void cancel(String reason, Instant now) {
        requireEditable();
        s = with(b -> {
            b.status = AssessmentStatus.CANCELLED;
            b.cancelledAt = now;
            b.cancelReason = clean(reason, 500, "Motivo");
        }, now);
    }

    public void requireEditable() {
        if (s.status().isFinal()) {
            throw new TrichologyStateException(s.status() == AssessmentStatus.PUBLISHED
                    ? "Relatório publicado não pode ser alterado; gere uma nova versão."
                    : "Exame cancelado não pode ser alterado.");
        }
    }

    /** Coleta nova move o status para frente; com relatório em revisão, volta à coleta. */
    private void advance(AssessmentStatus target, Instant now) {
        AssessmentStatus current = s.status();
        if (current.hasReportStage() || target.rank() > current.rank()) {
            s = with(b -> b.status = target, now);
        }
    }

    private static String clean(String value, int max, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        if (v.length() > max) {
            throw new TrichologyValidationException(field + " com no máximo " + max + " caracteres.");
        }
        return v;
    }

    // ------------------------------------------------------------- utilidades

    private State with(java.util.function.Consumer<Builder> change, Instant now) {
        Builder b = new Builder(s);
        change.accept(b);
        b.updatedAt = now;
        return b.build();
    }

    /** Cópia mutável do estado para as transições. */
    private static final class Builder {
        AssessmentStatus status;
        String chiefComplaint;
        Map<String, Object> quizAnswers;
        Instant quizCompletedAt;
        String quizFormTokenHash;
        Instant quizFormExpiresAt;
        String clinicalNotes;
        PullTest pullTest;
        boolean trichoscopyPerformed;
        List<SkippedRegion> skippedRegions;
        List<String> requestedExams;
        List<LabResult> labResults;
        LocalDate labCollectedOn;
        RpsSummary rpsSummary;
        ReportPayload aiDraftPayload;
        ReportPayload finalReportPayload;
        UUID patientDocumentId;
        String verificationCode;
        String pdfSha256;
        UUID pdfStorageObjectId;
        String professionalName;
        String professionalRegistry;
        UUID reviewedBy;
        Instant reviewedAt;
        Instant publishedAt;
        Instant cancelledAt;
        String cancelReason;
        Instant updatedAt;
        private final State o;

        Builder(State o) {
            this.o = o;
            status = o.status();
            chiefComplaint = o.chiefComplaint();
            quizAnswers = o.quizAnswers();
            quizCompletedAt = o.quizCompletedAt();
            quizFormTokenHash = o.quizFormTokenHash();
            quizFormExpiresAt = o.quizFormExpiresAt();
            clinicalNotes = o.clinicalNotes();
            pullTest = o.pullTest();
            trichoscopyPerformed = o.trichoscopyPerformed();
            skippedRegions = o.skippedRegions() == null ? new ArrayList<>() : o.skippedRegions();
            requestedExams = o.requestedExams();
            labResults = o.labResults();
            labCollectedOn = o.labCollectedOn();
            rpsSummary = o.rpsSummary();
            aiDraftPayload = o.aiDraftPayload();
            finalReportPayload = o.finalReportPayload();
            patientDocumentId = o.patientDocumentId();
            verificationCode = o.verificationCode();
            pdfSha256 = o.pdfSha256();
            pdfStorageObjectId = o.pdfStorageObjectId();
            professionalName = o.professionalName();
            professionalRegistry = o.professionalRegistry();
            reviewedBy = o.reviewedBy();
            reviewedAt = o.reviewedAt();
            publishedAt = o.publishedAt();
            cancelledAt = o.cancelledAt();
            cancelReason = o.cancelReason();
            updatedAt = o.updatedAt();
        }

        State build() {
            return new State(o.id(), o.companyId(), o.contactId(), o.professionalId(), o.protocolId(), status,
                    o.consentAt(), o.consentBy(), chiefComplaint, quizAnswers, quizCompletedAt, quizFormTokenHash,
                    quizFormExpiresAt, clinicalNotes, pullTest, trichoscopyPerformed, skippedRegions,
                    requestedExams, labResults, labCollectedOn, rpsSummary, aiDraftPayload, finalReportPayload,
                    o.reportVersion(), o.supersedesId(), patientDocumentId, verificationCode, pdfSha256,
                    pdfStorageObjectId, professionalName, professionalRegistry, reviewedBy, reviewedAt,
                    publishedAt, cancelledAt, cancelReason, o.createdBy(), o.createdAt(), updatedAt);
        }
    }

    // --------------------------------------------------------------- getters

    public UUID getId() { return s.id(); }
    public UUID getCompanyId() { return s.companyId(); }
    public UUID getContactId() { return s.contactId(); }
    public AssessmentStatus getStatus() { return s.status(); }
    public boolean isTrichoscopyPerformed() { return s.trichoscopyPerformed(); }
}
