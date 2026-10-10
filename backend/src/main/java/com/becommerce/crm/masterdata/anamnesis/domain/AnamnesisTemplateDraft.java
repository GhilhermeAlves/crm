package com.becommerce.crm.masterdata.anamnesis.domain;

import java.util.List;

/**
 * Estrutura completa e não persistida de um modelo de anamnese, usada para
 * provisionar o modelo padrão de uma empresa em uma única operação.
 */
public record AnamnesisTemplateDraft(
        AnamnesisModel model,
        List<SectionDraft> sections) {

    public record SectionDraft(AnamnesisSection section, List<QuestionDraft> questions) {}

    public record QuestionDraft(AnamnesisQuestion question, List<AnamnesisQuestionOption> options) {}
}
