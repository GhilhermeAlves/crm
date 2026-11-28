package com.becommerce.crm.clinical.trichology.domain;

/**
 * Situação do exame tricológico (doc v1.2, seção 2). As etapas de coleta (quiz,
 * captura, clínico, laboratório) não são obrigatórias nem sequenciais: o status
 * guarda o ponto mais avançado alcançado. Relatório e publicação têm regras
 * próprias em {@link TrichologyAssessment}.
 */
public enum AssessmentStatus {
    DRAFT(0),
    QUIZ_COMPLETED(1),
    IMAGING_IN_PROGRESS(2),
    IMAGING_DONE(3),
    CLINICAL_DONE(4),
    EXAMS_REQUESTED(5),
    LABS_LOADED(6),
    AI_DRAFT_READY(7),
    GUARDRAIL_BLOCKED(7),
    REVIEWED(8),
    PUBLISHED(9),
    CANCELLED(9);

    private final int rank;

    AssessmentStatus(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }

    /** Publicado ou cancelado: nada mais muda neste registro. */
    public boolean isFinal() {
        return this == PUBLISHED || this == CANCELLED;
    }

    /** Já tem relatório em revisão; novas coletas invalidam o rascunho. */
    public boolean hasReportStage() {
        return this == AI_DRAFT_READY || this == GUARDRAIL_BLOCKED || this == REVIEWED;
    }
}
