package com.becommerce.crm.masterdata.anamnesis.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnamnesisDefaultTemplateTest {

    @Test
    void buildsSixSectionsWithQuestions() {
        var draft = AnamnesisDefaultTemplate.build(UUID.randomUUID(), UUID.randomUUID());

        assertEquals(6, draft.sections().size());
        assertTrue(draft.model().isDefault());
        assertTrue(draft.model().isActive());
        assertEquals(AnamnesisDefaultTemplate.DEFAULT_NAME, draft.model().getName());

        int totalQuestions = draft.sections().stream()
                .mapToInt(section -> section.questions().size())
                .sum();
        assertTrue(totalQuestions >= 45, "esperado >= 45 perguntas, veio " + totalQuestions);

        draft.sections().forEach(section ->
                assertFalse(section.questions().isEmpty(), "seção sem perguntas: " + section.section().getTitle()));
    }

    @Test
    void hasOneProfessionalSection() {
        var draft = AnamnesisDefaultTemplate.build(UUID.randomUUID(), UUID.randomUUID());

        long professionalCount = draft.sections().stream()
                .filter(section -> section.section().isProfessional())
                .count();
        assertEquals(1, professionalCount);
    }

    @Test
    void allergyQuestionIsHighlightedWithComplement() {
        var draft = AnamnesisDefaultTemplate.build(UUID.randomUUID(), UUID.randomUUID());

        boolean found = draft.sections().stream()
                .flatMap(section -> section.questions().stream())
                .anyMatch(question -> "Possui alergia a algum medicamento?".equals(
                                question.question().getText())
                        && question.question().isHighlight()
                        && question.question().isAllowComplement());

        assertTrue(found, "a pergunta de alergia a medicamentos deve estar destacada e com complemento");
    }

    @Test
    void brushingFrequencyIsSingleSelectWithFiveOptions() {
        var draft = AnamnesisDefaultTemplate.build(UUID.randomUUID(), UUID.randomUUID());

        var question = draft.sections().stream()
                .flatMap(section -> section.questions().stream())
                .filter(q -> q.question().getType() == AnamnesisQuestionType.SINGLE_SELECT)
                .findFirst()
                .orElseThrow();

        assertEquals(5, question.options().size());
        assertEquals(AnamnesisQuestionType.SINGLE_SELECT, question.question().getType());
    }

    @Test
    void questionsAndOptionsCarryCompanyId() {
        UUID companyId = UUID.randomUUID();
        var draft = AnamnesisDefaultTemplate.build(companyId, UUID.randomUUID());

        assertEquals(companyId, draft.model().getCompanyId());
        draft.sections().forEach(section -> {
            assertEquals(companyId, section.section().getCompanyId());
            section.questions().forEach(question -> {
                assertEquals(companyId, question.question().getCompanyId());
                question.options().forEach(option -> assertEquals(companyId, option.getCompanyId()));
            });
        });
        List.of();
    }
}
