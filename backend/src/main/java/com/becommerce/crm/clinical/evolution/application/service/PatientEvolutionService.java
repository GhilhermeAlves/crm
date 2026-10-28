package com.becommerce.crm.clinical.evolution.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.clinical.evolution.application.dto.PatientEvolutionRequest;
import com.becommerce.crm.clinical.evolution.application.dto.PatientEvolutionResponse;
import com.becommerce.crm.clinical.evolution.application.port.in.PatientEvolutionUseCase;
import com.becommerce.crm.clinical.evolution.application.port.out.PatientEvolutionRepository;
import com.becommerce.crm.clinical.evolution.domain.PatientEvolution;
import com.becommerce.crm.clinical.evolution.domain.exception.PatientEvolutionNotFoundException;
import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.identity.domain.User;
import com.becommerce.crm.identity.domain.exception.CrmAccessDeniedException;
import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactNotFoundException;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException;
import com.becommerce.crm.sales.scheduling.application.port.out.AppointmentRepository;
import com.becommerce.crm.sales.scheduling.domain.Appointment;
import com.becommerce.crm.masterdata.contact.note.application.service.PatientTimelineRecorder;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Evoluções clínicas do paciente (ficha do paciente, Fase 2). Rascunhos podem
 * ser editados pelo autor ou por administradores; a finalização é exclusiva do
 * autor e torna o registro imutável. Cada operação isola a empresa ativa no
 * {@link TenantContext} e valida que contato e consulta pertencem à empresa.
 */
@Service
public class PatientEvolutionService implements PatientEvolutionUseCase {

    private static final String RESOURCE = "PatientEvolution";

    private final PatientEvolutionRepository evolutionRepository;
    private final ContactRepository contactRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final TenantAuditRecorder auditor;


    private PatientTimelineRecorder timeline;

    /** Opcional: registra os eventos nas anotações do paciente. */
    @Autowired(required = false)
    public void setPatientTimelineRecorder(PatientTimelineRecorder timeline) {
        this.timeline = timeline;
    }

    private void note(UUID companyId, UUID contactId, String body, UUID actorId) {
        if (timeline != null) {
            timeline.record(companyId, contactId, body, actorId);
        }
    }

    public PatientEvolutionService(PatientEvolutionRepository evolutionRepository,
                                   ContactRepository contactRepository,
                                   AppointmentRepository appointmentRepository,
                                   UserRepository userRepository,
                                   TenantAuditRecorder auditor) {
        this.evolutionRepository = evolutionRepository;
        this.contactRepository = contactRepository;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.auditor = auditor;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientEvolutionResponse> list(UUID companyId, UUID contactId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            Map<UUID, String> names = new HashMap<>();
            return evolutionRepository.findByCompanyIdAndContactIdOrderByCreatedAtDesc(companyId, contactId)
                    .stream()
                    .map(evolution -> toResponse(evolution, names))
                    .toList();
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public PatientEvolutionResponse create(UUID companyId, UUID contactId,
                                           PatientEvolutionRequest request, UUID createdBy) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            validateAppointment(companyId, contactId, request.appointmentId());
            PatientEvolution evolution = PatientEvolution.create(companyId, contactId, request.body(),
                    request.appointmentId(), createdBy);
            evolutionRepository.save(evolution);

            note(companyId, contactId, "Evolução registrada: " + evolution.getBody(), createdBy);
            auditor.record(companyId, AuditAction.CREATE, AuditModule.CONTACTS, RESOURCE,
                    evolution.getId().toString(), "Evolução criada para o paciente.", createdBy, null);
            return toResponse(evolution, new HashMap<>());
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public PatientEvolutionResponse update(UUID companyId, UUID contactId, UUID evolutionId,
                                           PatientEvolutionRequest request, UUID actorId, boolean admin) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            PatientEvolution evolution = requireOwned(companyId, contactId, evolutionId);
            assertCanModify(evolution, actorId, admin);
            validateAppointment(companyId, contactId, request.appointmentId());
            evolution.edit(request.body(), request.appointmentId());
            evolutionRepository.save(evolution);

            auditor.record(companyId, AuditAction.UPDATE, AuditModule.CONTACTS, RESOURCE,
                    evolution.getId().toString(), "Evolução atualizada.", actorId, null);
            return toResponse(evolution, new HashMap<>());
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public PatientEvolutionResponse finalizeEvolution(UUID companyId, UUID contactId, UUID evolutionId,
                                                      UUID actorId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            PatientEvolution evolution = requireOwned(companyId, contactId, evolutionId);
            if (!evolution.isAuthoredBy(actorId)) {
                throw new CrmAccessDeniedException("Só o autor pode finalizar esta evolução.");
            }
            evolution.finalizeBy(actorId);
            evolutionRepository.save(evolution);

            note(companyId, contactId, "Evolução finalizada: " + evolution.getBody(), actorId);
            auditor.record(companyId, AuditAction.UPDATE, AuditModule.CONTACTS, RESOURCE,
                    evolution.getId().toString(), "Evolução finalizada.", actorId, null);
            return toResponse(evolution, new HashMap<>());
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void delete(UUID companyId, UUID contactId, UUID evolutionId, UUID actorId, boolean admin) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            PatientEvolution evolution = requireOwned(companyId, contactId, evolutionId);
            assertCanModify(evolution, actorId, admin);
            evolutionRepository.delete(evolution);

            note(companyId, contactId, "Evolução excluída.", actorId);
            auditor.record(companyId, AuditAction.DELETE, AuditModule.CONTACTS, RESOURCE,
                    evolutionId.toString(), "Evolução excluída do paciente.", actorId, null);
        } finally {
            TenantContext.clear();
        }
    }

    private static void assertCanModify(PatientEvolution evolution, UUID actorId, boolean admin) {
        if (!evolution.isDraft()) {
            throw new ContactValidationException("Evolução finalizada não pode ser alterada.");
        }
        if (!admin && !evolution.isAuthoredBy(actorId)) {
            throw new CrmAccessDeniedException(
                    "Você só pode editar ou excluir as evoluções criadas por você.");
        }
    }

    private void validateAppointment(UUID companyId, UUID contactId, UUID appointmentId) {
        if (appointmentId == null) {
            return;
        }
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ContactValidationException(
                        "Consulta não encontrada para este paciente."));
        if (!appointment.getCompanyId().equals(companyId)
                || !contactId.equals(appointment.getContactId())) {
            throw new ContactValidationException("Consulta não encontrada para este paciente.");
        }
    }

    private void requireOwnedContact(UUID companyId, UUID contactId) {
        var contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ContactNotFoundException(contactId));
        if (!contact.getCompanyId().equals(companyId)) {
            throw new ContactNotFoundException(contactId);
        }
    }

    private PatientEvolution requireOwned(UUID companyId, UUID contactId, UUID evolutionId) {
        PatientEvolution evolution = evolutionRepository.findById(evolutionId)
                .orElseThrow(() -> new PatientEvolutionNotFoundException(evolutionId));
        if (!evolution.getCompanyId().equals(companyId)
                || !evolution.getContactId().equals(contactId)) {
            throw new PatientEvolutionNotFoundException(evolutionId);
        }
        return evolution;
    }

    private PatientEvolutionResponse toResponse(PatientEvolution evolution, Map<UUID, String> cache) {
        return new PatientEvolutionResponse(
                evolution.getId(),
                evolution.getContactId(),
                evolution.getAppointmentId(),
                evolution.getBody(),
                evolution.getStatus(),
                evolution.getCreatedBy(),
                userName(evolution.getCreatedBy(), cache),
                evolution.getFinalizedBy(),
                userName(evolution.getFinalizedBy(), cache),
                evolution.getFinalizedAt(),
                evolution.getCreatedAt(),
                evolution.getUpdatedAt());
    }

    private String userName(UUID userId, Map<UUID, String> cache) {
        if (userId == null) {
            return null;
        }
        if (cache.containsKey(userId)) {
            return cache.get(userId);
        }
        String name = userRepository.findById(userId).map(User::getName).orElse(null);
        cache.put(userId, name);
        return name;
    }
}
