package com.becommerce.crm.clinical.evolution.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.clinical.evolution.application.dto.PatientEvolutionRequest;
import com.becommerce.crm.clinical.evolution.application.dto.PatientEvolutionResponse;
import com.becommerce.crm.clinical.evolution.application.port.out.PatientEvolutionRepository;
import com.becommerce.crm.clinical.evolution.domain.PatientEvolution;
import com.becommerce.crm.clinical.evolution.domain.PatientEvolutionStatus;
import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.identity.domain.exception.CrmAccessDeniedException;
import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.masterdata.contact.domain.Contact;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException;
import com.becommerce.crm.sales.scheduling.application.port.out.AppointmentRepository;
import com.becommerce.crm.sales.scheduling.domain.Appointment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientEvolutionServiceTest {

    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID CONTACT_ID = UUID.randomUUID();
    private static final UUID AUTHOR_ID = UUID.randomUUID();
    private static final UUID OTHER_ID = UUID.randomUUID();

    @Mock
    private PatientEvolutionRepository repository;

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantAuditRecorder auditor;

    private PatientEvolutionService service;

    @BeforeEach
    void setUp() {
        service = new PatientEvolutionService(repository, contactRepository, appointmentRepository,
                userRepository, auditor);
        Contact contact = mock(Contact.class);
        lenient().when(contact.getCompanyId()).thenReturn(COMPANY_ID);
        lenient().when(contactRepository.findById(CONTACT_ID)).thenReturn(Optional.of(contact));
        lenient().when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsDraftWithAuthor() {
        PatientEvolutionResponse response = service.create(COMPANY_ID, CONTACT_ID,
                new PatientEvolutionRequest("Paciente sem queixas.", null), AUTHOR_ID);

        assertThat(response.status()).isEqualTo(PatientEvolutionStatus.DRAFT);
        assertThat(response.body()).isEqualTo("Paciente sem queixas.");
        assertThat(response.createdBy()).isEqualTo(AUTHOR_ID);
    }

    @Test
    void editsDraftAndUpdatesBody() {
        PatientEvolution evolution = draft(null);
        when(repository.findById(evolution.getId())).thenReturn(Optional.of(evolution));

        PatientEvolutionResponse response = service.update(COMPANY_ID, CONTACT_ID, evolution.getId(),
                new PatientEvolutionRequest("Novo texto.", null), AUTHOR_ID, false);

        assertThat(response.body()).isEqualTo("Novo texto.");
        assertThat(response.status()).isEqualTo(PatientEvolutionStatus.DRAFT);
    }

    @Test
    void finalizesOnlyByAuthor() {
        PatientEvolution evolution = draft(null);
        when(repository.findById(evolution.getId())).thenReturn(Optional.of(evolution));

        PatientEvolutionResponse response = service.finalizeEvolution(COMPANY_ID, CONTACT_ID,
                evolution.getId(), AUTHOR_ID);

        assertThat(response.status()).isEqualTo(PatientEvolutionStatus.FINALIZED);
        assertThat(response.finalizedBy()).isEqualTo(AUTHOR_ID);
        assertThat(response.finalizedAt()).isNotNull();
    }

    @Test
    void adminCannotFinalizeFromAnotherAuthor() {
        PatientEvolution evolution = draft(null);
        when(repository.findById(evolution.getId())).thenReturn(Optional.of(evolution));

        assertThatThrownBy(() -> service.finalizeEvolution(COMPANY_ID, CONTACT_ID,
                evolution.getId(), OTHER_ID))
                .isInstanceOf(CrmAccessDeniedException.class)
                .hasMessageContaining("Só o autor");
    }

    @Test
    void editingFinalizedEvolutionIsRejected() {
        PatientEvolution evolution = draft(null);
        evolution.finalizeBy(AUTHOR_ID);
        when(repository.findById(evolution.getId())).thenReturn(Optional.of(evolution));

        assertThatThrownBy(() -> service.update(COMPANY_ID, CONTACT_ID, evolution.getId(),
                new PatientEvolutionRequest("Texto novo.", null), AUTHOR_ID, true))
                .isInstanceOf(ContactValidationException.class)
                .hasMessageContaining("finalizada");
    }

    @Test
    void deletingFinalizedEvolutionIsRejected() {
        PatientEvolution evolution = draft(null);
        evolution.finalizeBy(AUTHOR_ID);
        when(repository.findById(evolution.getId())).thenReturn(Optional.of(evolution));

        assertThatThrownBy(() -> service.delete(COMPANY_ID, CONTACT_ID, evolution.getId(), AUTHOR_ID, true))
                .isInstanceOf(ContactValidationException.class)
                .hasMessageContaining("finalizada");
    }

    @Test
    void rejectsAppointmentFromAnotherPatient() {
        UUID appointmentId = UUID.randomUUID();
        Appointment appointment = mock(Appointment.class);
        when(appointment.getCompanyId()).thenReturn(COMPANY_ID);
        when(appointment.getContactId()).thenReturn(UUID.randomUUID());
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.create(COMPANY_ID, CONTACT_ID,
                new PatientEvolutionRequest("Texto.", appointmentId), AUTHOR_ID))
                .isInstanceOf(ContactValidationException.class)
                .hasMessageContaining("Consulta não encontrada");
    }

    @Test
    void rejectsMissingAppointment() {
        UUID appointmentId = UUID.randomUUID();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(COMPANY_ID, CONTACT_ID,
                new PatientEvolutionRequest("Texto.", appointmentId), AUTHOR_ID))
                .isInstanceOf(ContactValidationException.class)
                .hasMessageContaining("Consulta não encontrada");
    }

    private PatientEvolution draft(UUID appointmentId) {
        return PatientEvolution.create(COMPANY_ID, CONTACT_ID, "Texto inicial.", appointmentId, AUTHOR_ID);
    }
}
