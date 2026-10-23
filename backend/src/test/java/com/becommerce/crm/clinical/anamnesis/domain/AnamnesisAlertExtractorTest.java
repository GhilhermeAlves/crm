package com.becommerce.crm.clinical.anamnesis.domain;

import com.becommerce.crm.clinical.anamnesis.application.dto.PatientAnamnesisAnswer;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisModelResponse;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisOptionResponse;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisQuestionResponse;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisSectionResponse;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisQuestionType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AnamnesisAlertExtractorTest {

    private static final UUID COMPANY_ID = UUID.randomUUID();

    @Test
    void ignoresQuestionsWithoutHighlight() {
        AnamnesisQuestionResponse question = question(AnamnesisQuestionType.YES_NO, false, "Usa prótese?");
        Map<UUID, PatientAnamnesisAnswer> answers = Map.of(question.id(),
                new PatientAnamnesisAnswer(true, null));

        assertThat(AnamnesisAlertExtractor.extract(snapshot(question), answers)).isEmpty();
    }

    @Test
    void yesNoPositiveCreatesAlert() {
        AnamnesisQuestionResponse question = question(AnamnesisQuestionType.YES_NO, true, "Alergia?");
        Map<UUID, PatientAnamnesisAnswer> answers = Map.of(question.id(),
                new PatientAnamnesisAnswer(true, null));

        assertThat(AnamnesisAlertExtractor.extract(snapshot(question), answers))
                .containsExactly("Alergia?");
    }

    @Test
    void yesNoFalseOrIgnoreCaseStringDoesNotCreateAlert() {
        AnamnesisQuestionResponse question = question(AnamnesisQuestionType.YES_NO, true, "Alergia?");

        assertThat(AnamnesisAlertExtractor.extract(snapshot(question),
                Map.of(question.id(), new PatientAnamnesisAnswer(false, null)))).isEmpty();
        assertThat(AnamnesisAlertExtractor.extract(snapshot(question),
                Map.of(question.id(), new PatientAnamnesisAnswer("NAO", null)))).isEmpty();
        assertThat(AnamnesisAlertExtractor.extract(snapshot(question),
                Map.of(question.id(), new PatientAnamnesisAnswer("sim", null)))).containsExactly("Alergia?");
    }

    @Test
    void textValueUsesComplementWhenPresent() {
        AnamnesisQuestionResponse question = question(AnamnesisQuestionType.SHORT_TEXT, true, "Medicamentos?");
        Map<UUID, PatientAnamnesisAnswer> answers = Map.of(question.id(),
                new PatientAnamnesisAnswer("Losartana", "Uso contínuo"));

        assertThat(AnamnesisAlertExtractor.extract(snapshot(question), answers))
                .containsExactly("Medicamentos?: Uso contínuo");
    }

    @Test
    void selectWithoutComplementUsesOptionLabels() {
        AnamnesisQuestionResponse single = new AnamnesisQuestionResponse(UUID.randomUUID(),
                "Condição", AnamnesisQuestionType.SINGLE_SELECT, false, true, false, null, null, null, 0,
                List.of(new AnamnesisOptionResponse(UUID.randomUUID(), "Hipertensão", "hypertension", 0),
                        new AnamnesisOptionResponse(UUID.randomUUID(), "Diabetes", "diabetes", 1)));
        AnamnesisQuestionResponse multi = new AnamnesisQuestionResponse(UUID.randomUUID(),
                "Comorbidades", AnamnesisQuestionType.MULTI_SELECT, false, true, false, null, null, null, 1,
                List.of(new AnamnesisOptionResponse(UUID.randomUUID(), "Hipertensão", "hypertension", 0),
                        new AnamnesisOptionResponse(UUID.randomUUID(), "Diabetes", "diabetes", 1)));

        Map<UUID, PatientAnamnesisAnswer> answers = Map.of(
                single.id(), new PatientAnamnesisAnswer("hypertension", null),
                multi.id(), new PatientAnamnesisAnswer(List.of("hypertension", "diabetes"), null));

        assertThat(AnamnesisAlertExtractor.extract(snapshot(single, multi), answers))
                .containsExactly("Condição: Hipertensão", "Comorbidades: Hipertensão, Diabetes");
    }

    @Test
    void ignoresUnknownQuestionsAndBlankAnswers() {
        AnamnesisQuestionResponse question = question(AnamnesisQuestionType.LONG_TEXT, true, "Observações");

        assertThat(AnamnesisAlertExtractor.extract(snapshot(question),
                Map.of(question.id(), new PatientAnamnesisAnswer("   ", null)))).isEmpty();
        assertThat(AnamnesisAlertExtractor.extract(snapshot(question),
                Map.of(UUID.randomUUID(), new PatientAnamnesisAnswer("x", null)))).isEmpty();
    }

    private static AnamnesisQuestionResponse question(AnamnesisQuestionType type, boolean highlight,
                                                      String text) {
        return new AnamnesisQuestionResponse(UUID.randomUUID(), text, type, false, highlight,
                false, null, null, null, 0, List.of());
    }

    private static AnamnesisModelResponse snapshot(AnamnesisQuestionResponse... questions) {
        return new AnamnesisModelResponse(UUID.randomUUID(), COMPANY_ID, "Modelo", null, true, true, 1,
                List.of(new AnamnesisSectionResponse(UUID.randomUUID(), "Geral", false, 0,
                        List.of(questions))),
                LocalDateTime.now(), LocalDateTime.now());
    }
}
