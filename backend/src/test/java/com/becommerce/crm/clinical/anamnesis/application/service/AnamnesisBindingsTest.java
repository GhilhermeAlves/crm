package com.becommerce.crm.clinical.anamnesis.application.service;

import com.becommerce.crm.clinical.anamnesis.application.dto.PatientAnamnesisAnswer;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisBinding;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisQuestionType;
import com.becommerce.crm.masterdata.person.PersonService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static com.becommerce.crm.clinical.anamnesis.application.service.AnamnesisTestSnapshots.q;
import static com.becommerce.crm.clinical.anamnesis.application.service.AnamnesisTestSnapshots.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnamnesisBindingsTest {

    private static final UUID NAME = UUID.randomUUID();
    private static final UUID BIRTH = UUID.randomUUID();
    private static final UUID SEX = UUID.randomUUID();
    private static final UUID FREE = UUID.randomUUID();
    private static final UUID ONSET = UUID.randomUUID();
    private static final UUID FAMILY = UUID.randomUUID();

    @Test
    void prefillMapsProfileToBoundQuestions() {
        var snap = snapshot(q(NAME, "Nome", AnamnesisQuestionType.SHORT_TEXT, AnamnesisBinding.PERSON_NAME),
                q(BIRTH, "Nascimento", AnamnesisQuestionType.DATE, AnamnesisBinding.PERSON_BIRTH_DATE),
                q(SEX, "Sexo", AnamnesisQuestionType.SINGLE_SELECT, AnamnesisBinding.PERSON_SEX, "F|Feminino",
                        "M|Masculino"),
                q(FREE, "Fuma?", AnamnesisQuestionType.YES_NO, null));
        var profile = new PersonService.ProfileValues("Ana", LocalDate.of(1990, 5, 10), "Feminino", null, null,
                null, null);

        Map<UUID, Object> prefill = AnamnesisBindings.prefill(snap, profile);

        assertEquals("Ana", prefill.get(NAME));
        assertEquals("1990-05-10", prefill.get(BIRTH));
        assertEquals("F", prefill.get(SEX));
        assertFalse(prefill.containsKey(FREE));
    }

    @Test
    void valuesReadAnswersByBinding() {
        var snap = snapshot(q(ONSET, "Início", AnamnesisQuestionType.SINGLE_SELECT, AnamnesisBinding.TRICHO_ONSET),
                q(FAMILY, "Família", AnamnesisQuestionType.YES_NO, AnamnesisBinding.TRICHO_FAMILY_HISTORY),
                q(NAME, "Nome", AnamnesisQuestionType.SHORT_TEXT, AnamnesisBinding.PERSON_NAME));
        var answers = Map.of(ONSET, new PatientAnamnesisAnswer("LT_3M", null),
                FAMILY, new PatientAnamnesisAnswer(true, null),
                NAME, new PatientAnamnesisAnswer("  ", null));

        var values = AnamnesisBindings.values(snap, answers);

        assertEquals("LT_3M", values.get(AnamnesisBinding.TRICHO_ONSET));
        assertEquals(Boolean.TRUE, values.get(AnamnesisBinding.TRICHO_FAMILY_HISTORY));
        assertFalse(values.containsKey(AnamnesisBinding.PERSON_NAME));
    }

    @Test
    void snapshotWithoutBindingsYieldsEmpty() {
        var snap = snapshot(q(FREE, "Fuma?", AnamnesisQuestionType.YES_NO, null));
        assertTrue(AnamnesisBindings.values(snap, Map.of(FREE, new PatientAnamnesisAnswer(true, null))).isEmpty());
    }

    @Test
    void sexCodesConvertBothWays() {
        assertEquals("F", AnamnesisBindings.toSexCode("FEMININO"));
        assertEquals("M", AnamnesisBindings.toSexCode("Masculino"));
        assertEquals("Feminino", AnamnesisBindings.fromSexCode("F"));
        assertEquals(null, AnamnesisBindings.toSexCode("Outro"));
    }
}
