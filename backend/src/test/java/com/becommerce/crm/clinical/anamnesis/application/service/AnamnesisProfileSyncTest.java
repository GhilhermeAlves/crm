package com.becommerce.crm.clinical.anamnesis.application.service;

import com.becommerce.crm.clinical.anamnesis.application.dto.PatientAnamnesisAnswer;
import com.becommerce.crm.clinical.anamnesis.divergence.ProfileDivergenceJpaEntity;
import com.becommerce.crm.clinical.anamnesis.divergence.ProfileDivergenceJpaRepository;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisBinding;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisQuestionType;
import com.becommerce.crm.masterdata.person.PersonException;
import com.becommerce.crm.masterdata.person.PersonService;
import com.becommerce.crm.masterdata.person.PersonService.ProfileField;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.UUID;

import static com.becommerce.crm.clinical.anamnesis.application.service.AnamnesisTestSnapshots.q;
import static com.becommerce.crm.clinical.anamnesis.application.service.AnamnesisTestSnapshots.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnamnesisProfileSyncTest {

    private static final UUID C = UUID.randomUUID();
    private static final UUID P = UUID.randomUUID();
    private static final UUID A = UUID.randomUUID();
    private static final UUID U = UUID.randomUUID();
    private static final UUID NAME = UUID.randomUUID();
    private static final UUID MOBILE = UUID.randomUUID();
    private static final UUID CPF = UUID.randomUUID();

    private final PersonService persons = mock(PersonService.class);
    private final ProfileDivergenceJpaRepository divergences = mock(ProfileDivergenceJpaRepository.class);
    private final AnamnesisProfileSync sync = new AnamnesisProfileSync(persons, divergences);

    private static PersonService.ProfileValues profile(String name, String mobile, String cpf) {
        return new PersonService.ProfileValues(name, null, null, cpf, mobile, null, null);
    }

    private static final com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisModelResponse SNAP = snapshot(
            q(NAME, "Nome", AnamnesisQuestionType.SHORT_TEXT, AnamnesisBinding.PERSON_NAME),
            q(MOBILE, "Celular", AnamnesisQuestionType.SHORT_TEXT, AnamnesisBinding.PERSON_MOBILE),
            q(CPF, "CPF", AnamnesisQuestionType.SHORT_TEXT, AnamnesisBinding.PERSON_CPF));

    private static Map<UUID, PatientAnamnesisAnswer> answer(UUID id, String value) {
        return Map.of(id, new PatientAnamnesisAnswer(value, null));
    }

    @Test
    void emptyProfileFieldIsFilled() {
        when(persons.profileValues(C, P)).thenReturn(profile(null, null, null));
        var result = sync.sync(C, P, A, SNAP, answer(NAME, "Ana Souza"), U);
        verify(persons).applyAnamnesisPatch(C, P, ProfileField.NAME, "Ana Souza", U, "Anamnese");
        assertEquals(1, result.applied());
        verify(divergences, never()).save(any());
    }

    @Test
    void equalValueAfterNormalizationIsIgnored() {
        when(persons.profileValues(C, P)).thenReturn(profile("Ana", "34999990000", null));
        sync.sync(C, P, A, SNAP, answer(MOBILE, "(34) 99999-0000"), U);
        verify(persons, never()).applyAnamnesisPatch(any(), any(), any(), any(), any(), any());
        verify(divergences, never()).save(any());
    }

    @Test
    void differentValueBecomesDivergence() {
        when(persons.profileValues(C, P)).thenReturn(profile("Ana", null, null));
        var result = sync.sync(C, P, A, SNAP, answer(NAME, "Ana Souza"), U);
        verify(persons, never()).applyAnamnesisPatch(any(), any(), any(), any(), any(), any());
        var captor = ArgumentCaptor.forClass(ProfileDivergenceJpaEntity.class);
        verify(divergences).save(captor.capture());
        assertEquals("NAME", captor.getValue().field);
        assertEquals("Ana", captor.getValue().currentValue);
        assertEquals("Ana Souza", captor.getValue().proposedValue);
        assertEquals(1, result.divergences());
    }

    @Test
    void invalidCpfBecomesDivergenceWithReason() {
        when(persons.profileValues(C, P)).thenReturn(profile("Ana", null, null));
        sync.sync(C, P, A, SNAP, answer(CPF, "123"), U);
        var captor = ArgumentCaptor.forClass(ProfileDivergenceJpaEntity.class);
        verify(divergences).save(captor.capture());
        assertEquals("CPF inválido.", captor.getValue().reason);
        verify(persons, never()).applyAnamnesisPatch(any(), any(), any(), any(), any(), any());
    }

    @Test
    void cpfOfAnotherPersonBecomesDivergence() {
        when(persons.profileValues(C, P)).thenReturn(profile("Ana", null, null));
        doThrow(PersonException.badRequest("Este CPF/CNPJ já está no cadastro de Bia."))
                .when(persons).applyAnamnesisPatch(eq(C), eq(P), eq(ProfileField.CPF), anyString(), any(), any());
        sync.sync(C, P, A, SNAP, answer(CPF, "529.982.247-25"), U);
        var captor = ArgumentCaptor.forClass(ProfileDivergenceJpaEntity.class);
        verify(divergences).save(captor.capture());
        assertEquals("Este CPF/CNPJ já está no cadastro de Bia.", captor.getValue().reason);
    }

    @Test
    void publicSubmissionUsesPatientOrigin() {
        when(persons.profileValues(C, P)).thenReturn(profile(null, null, null));
        sync.sync(C, P, A, SNAP, answer(NAME, "Ana"), null);
        verify(persons).applyAnamnesisPatch(C, P, ProfileField.NAME, "Ana", null, "Anamnese (paciente)");
    }
}
