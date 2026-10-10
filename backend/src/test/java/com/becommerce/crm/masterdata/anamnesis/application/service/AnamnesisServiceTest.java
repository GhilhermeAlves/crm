package com.becommerce.crm.masterdata.anamnesis.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisOptionRequest;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisQuestionRequest;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisSectionRequest;
import com.becommerce.crm.masterdata.anamnesis.application.dto.CreateAnamnesisModelRequest;
import com.becommerce.crm.masterdata.anamnesis.application.dto.UpdateAnamnesisModelRequest;
import com.becommerce.crm.masterdata.anamnesis.application.port.output.AnamnesisModelRepository;
import com.becommerce.crm.masterdata.anamnesis.application.port.output.AnamnesisQuestionOptionRepository;
import com.becommerce.crm.masterdata.anamnesis.application.port.output.AnamnesisQuestionRepository;
import com.becommerce.crm.masterdata.anamnesis.application.port.output.AnamnesisSectionRepository;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisComplementTrigger;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisModel;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisQuestionType;
import com.becommerce.crm.masterdata.anamnesis.domain.exception.AnamnesisModelNotFoundException;
import com.becommerce.crm.masterdata.anamnesis.domain.exception.AnamnesisValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnamnesisServiceTest {

    private static final UUID COMPANY = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();

    private AnamnesisModelRepository modelRepository;
    private AnamnesisSectionRepository sectionRepository;
    private AnamnesisQuestionRepository questionRepository;
    private AnamnesisQuestionOptionRepository optionRepository;
    private AnamnesisService service;

    @BeforeEach
    void setUp() {
        modelRepository = mock(AnamnesisModelRepository.class);
        sectionRepository = mock(AnamnesisSectionRepository.class);
        questionRepository = mock(AnamnesisQuestionRepository.class);
        optionRepository = mock(AnamnesisQuestionOptionRepository.class);
        service = new AnamnesisService(modelRepository, sectionRepository, questionRepository,
                optionRepository, mock(TenantAuditRecorder.class));
        when(sectionRepository.findByModelIdOrderBySortOrder(any())).thenReturn(List.of());
        when(questionRepository.findByModelIdOrderBySortOrder(any())).thenReturn(List.of());
        when(optionRepository.findByModelIdOrderBySortOrder(any())).thenReturn(List.of());
        when(modelRepository.save(any())).thenAnswer(inv -> {
            AnamnesisModel saved = inv.getArgument(0);
            when(modelRepository.findByCompanyIdOrderByName(eq(COMPANY))).thenReturn(List.of(saved));
            return saved;
        });
    }

    @Test
    void listProvisionsDefaultTemplateWhenCompanyHasNoModels() {
        when(modelRepository.existsByCompanyId(COMPANY)).thenReturn(false);

        var summaries = service.list(COMPANY, USER);

        verify(modelRepository).save(any());
        verify(sectionRepository, times(6)).save(any());
        assertEquals(1, summaries.size());
        assertEquals("Anamnese Odontológica Padrão", summaries.get(0).name());
    }

    @Test
    void listDoesNotProvisionWhenModelsAlreadyExist() {
        when(modelRepository.existsByCompanyId(COMPANY)).thenReturn(true);
        when(modelRepository.findByCompanyIdOrderByName(COMPANY)).thenReturn(List.of());

        service.list(COMPANY, USER);

        verify(modelRepository, never()).save(any());
    }

    @Test
    void createReturnsEmptyModel() {
        var response = service.create(COMPANY, new CreateAnamnesisModelRequest("Meu modelo", "desc"), USER);

        assertEquals("Meu modelo", response.name());
        assertEquals(COMPANY, response.companyId());
        assertFalse(response.isDefault());
        assertEquals(0, response.sections().size());
    }

    @Test
    void updateReplacesStructureAndBumpsVersion() {
        AnamnesisModel model = AnamnesisModel.create(UUID.randomUUID(), COMPANY, "Antigo", null, USER);
        int initialVersion = model.getVersion();
        when(modelRepository.findById(model.getId())).thenReturn(Optional.of(model));

        var sections = List.of(new AnamnesisSectionRequest(null, "Seção 1", false, List.of(
                new AnamnesisQuestionRequest(null, "Tem alergia?", AnamnesisQuestionType.YES_NO, null,
                        true, true, "Qual?", AnamnesisComplementTrigger.YES, null, null))));
        var response = service.update(COMPANY, model.getId(),
                new UpdateAnamnesisModelRequest("Novo nome", null, sections), USER);

        verify(questionRepository).deleteByModelId(model.getId());
        verify(sectionRepository).deleteByModelId(model.getId());
        verify(sectionRepository, times(1)).save(any());
        verify(questionRepository, times(1)).save(any());
        assertEquals("Novo nome", response.name());
        assertEquals(initialVersion + 1, response.version());
    }

    @Test
    void updateRejectsSelectWithoutOptions() {
        AnamnesisModel model = AnamnesisModel.create(UUID.randomUUID(), COMPANY, "M", null, USER);
        when(modelRepository.findById(model.getId())).thenReturn(Optional.of(model));

        var sections = List.of(new AnamnesisSectionRequest(null, "S", false, List.of(
                new AnamnesisQuestionRequest(null, "Escolha", AnamnesisQuestionType.SINGLE_SELECT, null,
                        null, null, null, null, null, List.of()))));

        assertThrows(AnamnesisValidationException.class, () -> service.update(COMPANY, model.getId(),
                new UpdateAnamnesisModelRequest("M", null, sections), USER));
    }

    @Test
    void updateRejectsComplementTriggerOptionNotAmongOptions() {
        AnamnesisModel model = AnamnesisModel.create(UUID.randomUUID(), COMPANY, "M", null, USER);
        when(modelRepository.findById(model.getId())).thenReturn(Optional.of(model));

        var sections = List.of(new AnamnesisSectionRequest(null, "S", false, List.of(
                new AnamnesisQuestionRequest(null, "Escolha", AnamnesisQuestionType.SINGLE_SELECT, null,
                        null, true, "Detalhe", AnamnesisComplementTrigger.OPTION, "x", List.of(
                        new AnamnesisOptionRequest(null, "Sim", "sim"))))));

        assertThrows(AnamnesisValidationException.class, () -> service.update(COMPANY, model.getId(),
                new UpdateAnamnesisModelRequest("M", null, sections), USER));
    }

    @Test
    void getByIdOfAnotherCompanyThrowsNotFound() {
        AnamnesisModel other = AnamnesisModel.create(UUID.randomUUID(), UUID.randomUUID(), "Outro", null, USER);
        when(modelRepository.findById(other.getId())).thenReturn(Optional.of(other));

        assertThrows(AnamnesisModelNotFoundException.class, () -> service.getById(COMPANY, other.getId()));
    }

    @Test
    void deleteRejectsDefaultModel() {
        AnamnesisModel def = AnamnesisModel.createDefault(UUID.randomUUID(), COMPANY, "Padrão", null, USER);
        when(modelRepository.findById(def.getId())).thenReturn(Optional.of(def));

        assertThrows(AnamnesisValidationException.class, () -> service.delete(COMPANY, def.getId(), USER));
        verify(modelRepository, never()).delete(any());
    }

    @Test
    void deleteRemovesNonDefaultModel() {
        AnamnesisModel model = AnamnesisModel.create(UUID.randomUUID(), COMPANY, "Comum", null, USER);
        when(modelRepository.findById(model.getId())).thenReturn(Optional.of(model));

        service.delete(COMPANY, model.getId(), USER);

        verify(questionRepository).deleteByModelId(model.getId());
        verify(sectionRepository).deleteByModelId(model.getId());
        verify(modelRepository).delete(model);
    }
}
