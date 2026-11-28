package com.becommerce.crm.masterdata.anamnesis.application.service;

import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisOptionRequest;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisQuestionRequest;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisSectionRequest;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisBinding;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisQuestionType;
import com.becommerce.crm.masterdata.anamnesis.domain.exception.AnamnesisValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnamnesisBindingValidationTest {

    @Test
    void trichoBindingsExposeQuizKeysAndCodes() {
        assertEquals("pattern", AnamnesisBinding.TRICHO_PATTERN.quizKey());
        assertTrue(AnamnesisBinding.TRICHO_PATTERN.codes().contains("DIFFUSE_SHEDDING"));
        assertEquals("sex", AnamnesisBinding.PERSON_SEX.quizKey());
        assertNull(AnamnesisBinding.PERSON_NAME.quizKey());
    }

    @Test
    void rejectsDuplicatedBinding() {
        var sections = section(question("Nome", AnamnesisQuestionType.SHORT_TEXT, AnamnesisBinding.PERSON_NAME),
                question("Nome de novo", AnamnesisQuestionType.SHORT_TEXT, AnamnesisBinding.PERSON_NAME));
        var ex = assertThrows(AnamnesisValidationException.class,
                () -> AnamnesisService.validateBindings(sections));
        assertTrue(ex.getMessage().contains("PERSON_NAME"));
    }

    @Test
    void rejectsIncompatibleType() {
        var sections = section(question("Nascimento", AnamnesisQuestionType.SHORT_TEXT,
                AnamnesisBinding.PERSON_BIRTH_DATE));
        assertThrows(AnamnesisValidationException.class, () -> AnamnesisService.validateBindings(sections));
    }

    @Test
    void rejectsUnknownOptionCode() {
        var q = new AnamnesisQuestionRequest(null, "Início", AnamnesisQuestionType.SINGLE_SELECT, true, false,
                false, null, null, null, List.of(new AnamnesisOptionRequest(null, "Ontem", "foo")),
                AnamnesisBinding.TRICHO_ONSET);
        var ex = assertThrows(AnamnesisValidationException.class,
                () -> AnamnesisService.validateBindings(section(q)));
        assertTrue(ex.getMessage().contains("LT_3M"));
    }

    @Test
    void acceptsValidBindings() {
        var onset = new AnamnesisQuestionRequest(null, "Início", AnamnesisQuestionType.SINGLE_SELECT, true, false,
                false, null, null, null, List.of(new AnamnesisOptionRequest(null, "Menos de 3 meses", "LT_3M")),
                AnamnesisBinding.TRICHO_ONSET);
        var name = question("Nome", AnamnesisQuestionType.SHORT_TEXT, AnamnesisBinding.PERSON_NAME);
        var free = question("Fuma?", AnamnesisQuestionType.YES_NO, null);
        assertDoesNotThrow(() -> AnamnesisService.validateBindings(section(onset, name, free)));
    }

    private static AnamnesisQuestionRequest question(String text, AnamnesisQuestionType type,
                                                     AnamnesisBinding binding) {
        return new AnamnesisQuestionRequest(null, text, type, false, false, false, null, null, null, List.of(),
                binding);
    }

    private static List<AnamnesisSectionRequest> section(AnamnesisQuestionRequest... questions) {
        return List.of(new AnamnesisSectionRequest(null, "Seção", false, List.of(questions)));
    }
}
