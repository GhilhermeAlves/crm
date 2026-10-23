package com.becommerce.crm.clinical.anamnesis.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.clinical.anamnesis.application.dto.PatientAnamnesisAnswer;
import com.becommerce.crm.clinical.anamnesis.application.dto.PatientAnamnesisResponse;
import com.becommerce.crm.clinical.anamnesis.application.dto.SavePatientAnamnesisRequest;
import com.becommerce.crm.clinical.anamnesis.application.dto.StartPatientAnamnesisRequest;
import com.becommerce.crm.clinical.anamnesis.application.port.out.PatientAnamnesisRepository;
import com.becommerce.crm.clinical.anamnesis.domain.PatientAnamnesis;
import com.becommerce.crm.clinical.anamnesis.domain.PatientAnamnesisStatus;
import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.identity.domain.exception.CrmAccessDeniedException;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisModelResponse;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisQuestionResponse;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisSectionResponse;
import com.becommerce.crm.masterdata.anamnesis.application.service.AnamnesisService;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisQuestionType;
import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.masterdata.contact.domain.Contact;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientAnamnesisServiceTest {

    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID CONTACT_ID = UUID.randomUUID();
    private static final UUID MODEL_ID = UUID.randomUUID();
    private static final UUID AUTHOR_ID = UUID.randomUUID();
    private static final UUID OTHER_ID = UUID.randomUUID();

    @Mock
    private PatientAnamnesisRepository repository;

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private AnamnesisService modelService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantAuditRecorder auditor;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private PatientAnamnesisService service;

    private Contact contact;

    @BeforeEach
    void setUp() {
        service = new PatientAnamnesisService(repository, contactRepository, modelService, userRepository,
                auditor, objectMapper);
        contact = mock(Contact.class);
        when(contact.getCompanyId()).thenReturn(COMPANY_ID);
        when(contactRepository.findById(CONTACT_ID)).thenReturn(Optional.of(contact));
        lenient().when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void startUsesDefaultModelAndStoresSnapshot() {
        AnamnesisModelResponse model = model();
        when(modelService.getDefaultModel(COMPANY_ID, AUTHOR_ID)).thenReturn(model);

        PatientAnamnesisResponse response = service.start(COMPANY_ID, CONTACT_ID,
                new StartPatientAnamnesisRequest(null), AUTHOR_ID);

        assertThat(response.modelId()).isEqualTo(MODEL_ID);
        assertThat(response.modelName()).isEqualTo("Modelo");
        assertThat(response.modelVersion()).isEqualTo(1);
        assertThat(response.snapshot().id()).isEqualTo(MODEL_ID);
        assertThat(response.status()).isEqualTo(PatientAnamnesisStatus.DRAFT);
        assertThat(response.createdBy()).isEqualTo(AUTHOR_ID);
        verify(repository).save(any());
    }

    @Test
    void startRejectsInactiveModel() {
        AnamnesisModelResponse inactive = new AnamnesisModelResponse(MODEL_ID, COMPANY_ID, "Modelo", null,
                false, true, 1, List.of(), LocalDateTime.now(), LocalDateTime.now());
        when(modelService.getById(COMPANY_ID, MODEL_ID)).thenReturn(inactive);

        assertThatThrownBy(() -> service.start(COMPANY_ID, CONTACT_ID,
                new StartPatientAnamnesisRequest(MODEL_ID), AUTHOR_ID))
                .isInstanceOf(ContactValidationException.class)
                .hasMessageContaining("inativo");
    }

    @Test
    void saveDraftKeepsOnlyQuestionsFromSnapshot() {
        AnamnesisModelResponse model = model();
        PatientAnamnesis draft = draft(model);
        when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));
        UUID known = model.sections().get(0).questions().get(0).id();
        Map<UUID, PatientAnamnesisAnswer> answers = new LinkedHashMap<>();
        answers.put(known, new PatientAnamnesisAnswer("Sim", null));
        answers.put(UUID.randomUUID(), new PatientAnamnesisAnswer("Ignorar", null));

        PatientAnamnesisResponse response = service.save(COMPANY_ID, CONTACT_ID, draft.getId(),
                new SavePatientAnamnesisRequest(answers, false), AUTHOR_ID, false);

        assertThat(response.answers()).containsOnlyKeys(known);
        assertThat(response.status()).isEqualTo(PatientAnamnesisStatus.DRAFT);
        assertThat(response.alerts()).isEmpty();
    }

    @Test
    void completeRequiresMandatoryAnswers() {
        AnamnesisModelResponse model = model();
        PatientAnamnesis draft = draft(model);
        when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));
        UUID required = model.sections().get(0).questions().get(0).id();

        assertThatThrownBy(() -> service.save(COMPANY_ID, CONTACT_ID, draft.getId(),
                new SavePatientAnamnesisRequest(Map.of(), true), AUTHOR_ID, false))
                .isInstanceOf(ContactValidationException.class)
                .hasMessageContaining("obrigatórias");
        assertThat(required).isNotNull();
    }

    @Test
    void completeValidatesAlertsAndLocksAnamnesis() {
        AnamnesisModelResponse model = model();
        PatientAnamnesis draft = draft(model);
        when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));
        UUID required = model.sections().get(0).questions().get(0).id();

        PatientAnamnesisResponse response = service.save(COMPANY_ID, CONTACT_ID, draft.getId(),
                new SavePatientAnamnesisRequest(Map.of(required,
                        new PatientAnamnesisAnswer(true, null)), true), AUTHOR_ID, false);

        assertThat(response.status()).isEqualTo(PatientAnamnesisStatus.COMPLETED);
        assertThat(response.completedBy()).isEqualTo(AUTHOR_ID);
        assertThat(response.alerts()).containsExactly("Alergia?");

        assertThatThrownBy(() -> service.save(COMPANY_ID, CONTACT_ID, draft.getId(),
                new SavePatientAnamnesisRequest(Map.of(), false), AUTHOR_ID, false))
                .isInstanceOf(ContactValidationException.class)
                .hasMessageContaining("concluída");
    }

    @Test
    void nonAuthorCannotEditDraft() {
        PatientAnamnesis draft = draft(model());
        when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.save(COMPANY_ID, CONTACT_ID, draft.getId(),
                new SavePatientAnamnesisRequest(Map.of(), false), OTHER_ID, false))
                .isInstanceOf(CrmAccessDeniedException.class);
    }

    @Test
    void adminCanEditDraftFromAnotherAuthor() {
        PatientAnamnesis draft = draft(model());
        when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

        PatientAnamnesisResponse response = service.save(COMPANY_ID, CONTACT_ID, draft.getId(),
                new SavePatientAnamnesisRequest(Map.of(), false), OTHER_ID, true);

        assertThat(response.status()).isEqualTo(PatientAnamnesisStatus.DRAFT);
    }

    @Test
    void deleteCompletedAnamnesisIsForbidden() {
        PatientAnamnesis draft = draft(model());
        draft.complete("{}", List.of(), AUTHOR_ID);
        when(repository.findById(draft.getId())).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.delete(COMPANY_ID, CONTACT_ID, draft.getId(), AUTHOR_ID, false))
                .isInstanceOf(ContactValidationException.class)
                .hasMessageContaining("excluída");
    }

    @Test
    void currentReturnsEmptyWhenNoCompletedAnamnesis() {
        when(repository.findFirstByCompanyIdAndContactIdAndStatusOrderByCompletedAtDesc(
                COMPANY_ID, CONTACT_ID, PatientAnamnesisStatus.COMPLETED)).thenReturn(Optional.empty());

        assertThat(service.current(COMPANY_ID, CONTACT_ID)).isEmpty();
    }

    private PatientAnamnesis draft(AnamnesisModelResponse snapshot) {
        return PatientAnamnesis.create(COMPANY_ID, CONTACT_ID, snapshot.id(), snapshot.name(),
                snapshot.version(), writeJson(snapshot), AUTHOR_ID);
    }

    private static AnamnesisModelResponse model() {
        AnamnesisQuestionResponse question = new AnamnesisQuestionResponse(UUID.randomUUID(), "Alergia?",
                AnamnesisQuestionType.YES_NO, true, true, false, null, null, null, 0, List.of());
        return new AnamnesisModelResponse(MODEL_ID, COMPANY_ID, "Modelo", null, true, true, 1,
                List.of(new AnamnesisSectionResponse(UUID.randomUUID(), "Geral", false, 0,
                        List.of(question))), LocalDateTime.now(), LocalDateTime.now());
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
