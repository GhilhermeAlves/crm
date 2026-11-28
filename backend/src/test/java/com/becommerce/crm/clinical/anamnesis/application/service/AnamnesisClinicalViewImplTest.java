package com.becommerce.crm.clinical.anamnesis.application.service;

import com.becommerce.crm.clinical.anamnesis.application.dto.PatientAnamnesisAnswer;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisBinding;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisQuestionType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.becommerce.crm.clinical.anamnesis.application.service.AnamnesisTestSnapshots.q;
import static com.becommerce.crm.clinical.anamnesis.application.service.AnamnesisTestSnapshots.section;
import static com.becommerce.crm.clinical.anamnesis.application.service.AnamnesisTestSnapshots.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnamnesisClinicalViewImplTest {

    private static final UUID ONSET = UUID.randomUUID();
    private static final UUID FAMILY = UUID.randomUUID();
    private static final UUID TRIGGERS = UUID.randomUUID();
    private static final UUID SEX = UUID.randomUUID();
    private static final UUID NAME = UUID.randomUUID();
    private static final UUID SMOKE = UUID.randomUUID();
    private static final UUID PRO = UUID.randomUUID();

    private final com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisModelResponse snap = snapshot(
            section("Dados", false,
                    q(NAME, "Nome completo", AnamnesisQuestionType.SHORT_TEXT, AnamnesisBinding.PERSON_NAME),
                    q(SEX, "Sexo", AnamnesisQuestionType.SINGLE_SELECT, AnamnesisBinding.PERSON_SEX,
                            "F|Feminino", "M|Masculino"),
                    q(SMOKE, "Fuma?", AnamnesisQuestionType.YES_NO, null)),
            section("Avaliação tricológica", false,
                    q(ONSET, "Início", AnamnesisQuestionType.SINGLE_SELECT, AnamnesisBinding.TRICHO_ONSET,
                            "LT_3M|Menos de 3 meses", "GT_1Y|Mais de 1 ano"),
                    q(FAMILY, "Família", AnamnesisQuestionType.YES_NO, AnamnesisBinding.TRICHO_FAMILY_HISTORY),
                    q(TRIGGERS, "Gatilhos", AnamnesisQuestionType.MULTI_SELECT,
                            AnamnesisBinding.TRICHO_RECENT_TRIGGERS, "SURGERY|Cirurgia")),
            section("Profissional", true, q(PRO, "Conduta", AnamnesisQuestionType.LONG_TEXT, null)));

    private final Map<UUID, PatientAnamnesisAnswer> answers = Map.of(
            NAME, new PatientAnamnesisAnswer("Ana Souza", null),
            SEX, new PatientAnamnesisAnswer("F", null),
            SMOKE, new PatientAnamnesisAnswer(false, null),
            ONSET, new PatientAnamnesisAnswer("LT_3M", null),
            FAMILY, new PatientAnamnesisAnswer(true, null),
            TRIGGERS, new PatientAnamnesisAnswer(List.of("SURGERY"), null),
            PRO, new PatientAnamnesisAnswer("texto do profissional", null));

    @Test
    void quizLikeUsesRuleKeysAndStringBooleans() {
        Map<String, Object> quiz = AnamnesisClinicalViewImpl.quizLike(snap, answers);
        assertEquals("LT_3M", quiz.get("onset"));
        assertEquals("true", quiz.get("family_history"));
        assertEquals(List.of("SURGERY"), quiz.get("recent_triggers"));
        assertEquals("F", quiz.get("sex"));
    }

    @Test
    void readableAnswersHidePersonDataAndProfessionalSections() {
        var readable = AnamnesisClinicalViewImpl.readable(snap, answers);
        assertTrue(readable.stream().noneMatch(a -> a.answer().contains("Ana")));
        assertTrue(readable.stream().noneMatch(a -> a.question().equals("Conduta")));
        assertTrue(readable.stream().anyMatch(a -> a.question().equals("Fuma?") && a.answer().equals("Não")));
        assertTrue(readable.stream().anyMatch(a -> a.question().equals("Início")
                && a.answer().equals("Menos de 3 meses")));
    }
}
