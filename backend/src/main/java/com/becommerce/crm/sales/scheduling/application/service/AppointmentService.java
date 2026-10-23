package com.becommerce.crm.sales.scheduling.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.masterdata.catalog.application.port.output.CatalogItemRepository;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItem;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItemType;
import com.becommerce.crm.sales.chair.application.port.in.ChairUseCase;
import com.becommerce.crm.sales.tag.application.port.in.TagUseCase;
import com.becommerce.crm.sales.scheduling.application.dto.*;
import com.becommerce.crm.sales.scheduling.application.port.in.AppointmentUseCase;
import com.becommerce.crm.sales.scheduling.application.port.out.AppointmentProcedureRepository;
import com.becommerce.crm.sales.scheduling.application.port.out.AppointmentRepository;
import com.becommerce.crm.sales.scheduling.application.port.out.AppointmentTagRepository;
import com.becommerce.crm.sales.scheduling.application.port.out.ScheduleBlockRepository;
import com.becommerce.crm.sales.scheduling.domain.*;
import com.becommerce.crm.sales.scheduling.domain.exception.SchedulingNotFoundException;
import com.becommerce.crm.sales.scheduling.domain.exception.SchedulingValidationException;
import com.becommerce.crm.sales.scheduling.domain.exception.SlotUnavailableException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import com.becommerce.crm.masterdata.contact.application.service.PatientAutoMarker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AppointmentService implements AppointmentUseCase {

    private static final Duration MAX_RANGE = Duration.ofDays(62);
    private static final int CONTACT_HISTORY_LIMIT = 200;

    private final AppointmentRepository appointmentRepository;
    private final ScheduleBlockRepository blockRepository;
    private final TenantAuditRecorder auditor;
    private final ChairUseCase chairUseCase;
    private final TagUseCase tagUseCase;
    private final AppointmentTagRepository tagRepository;
    private final CatalogItemRepository catalogItemRepository;
    private final AppointmentProcedureRepository procedureRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              ScheduleBlockRepository blockRepository,
                              TenantAuditRecorder auditor,
                              ChairUseCase chairUseCase,
                              TagUseCase tagUseCase,
                              AppointmentTagRepository tagRepository,
                              CatalogItemRepository catalogItemRepository,
                              AppointmentProcedureRepository procedureRepository) {
        this.appointmentRepository = appointmentRepository;
        this.blockRepository = blockRepository;
        this.auditor = auditor;
        this.chairUseCase = chairUseCase;
        this.tagUseCase = tagUseCase;
        this.tagRepository = tagRepository;
        this.catalogItemRepository = catalogItemRepository;
        this.procedureRepository = procedureRepository;
    }

    private PatientAutoMarker patientAutoMarker;

    /** Opcional: marca o contato como paciente ao agendar (módulo PATIENTS, V104). */
    @Autowired(required = false)
    public void setPatientAutoMarker(PatientAutoMarker patientAutoMarker) {
        this.patientAutoMarker = patientAutoMarker;
    }

    @Override
    @Transactional
    public AppointmentResponse create(UUID companyId, CreateAppointmentRequest request, UUID createdBy) {
        return create(companyId, request, createdBy, AppointmentSource.INTERNAL);
    }

    @Override
    @Transactional
    public AppointmentResponse create(UUID companyId, CreateAppointmentRequest request, UUID createdBy,
                                      AppointmentSource source) {
        try {
            TenantContext.setCompanyId(companyId);
            validateTags(companyId, request.tagIds());
            validateProcedures(companyId, request.catalogItemIds());
            boolean force = request.force() != null && request.force();
            List<Interval> occurrences = RecurrenceExpander.expand(request.startAt(), request.endAt(),
                    request.recurrence());
            if (occurrences.size() == 1) {
                checkOccurrence(request, companyId, force, request.startAt(), request.endAt());
            } else {
                // Recorrência: valida todas antes de gravar qualquer uma; conflito em
                // qualquer ocorrência → nada é criado e as datas são listadas.
                List<Instant> conflicts = new ArrayList<>();
                for (Interval occurrence : occurrences) {
                    try {
                        checkOccurrence(request, companyId, force, occurrence.start(), occurrence.end());
                    } catch (SlotUnavailableException e) {
                        conflicts.add(occurrence.start());
                    }
                }
                if (!conflicts.isEmpty()) {
                    throw new SlotUnavailableException(conflictMessage(conflicts));
                }
            }

            UUID groupId = occurrences.size() > 1 ? UUID.randomUUID() : null;
            Appointment appointment = null;
            for (Interval occurrence : occurrences) {
                Appointment created = Appointment.create(companyId, request.appointmentTypeId(),
                        request.hostId(), request.contactId(), request.opportunityId(),
                        request.title(), occurrence.start(), occurrence.end(),
                        source, request.locationKind(), request.locationDetail(),
                        request.notes(), createdBy, request.chairId());
                created.setRecurrenceGroupId(groupId);
                applyInitialStatus(created, request.status());
                appointmentRepository.save(created);
                if (request.tagIds() != null) {
                    tagRepository.replace(companyId, created.getId(), request.tagIds());
                }
                if (request.catalogItemIds() != null) {
                    procedureRepository.replace(companyId, created.getId(), request.catalogItemIds());
                }
                if (appointment == null) {
                    appointment = created;
                }
            }
            if (patientAutoMarker != null) {
                patientAutoMarker.markOnAppointment(companyId, request.contactId());
                // markAsPatientIfExists limpa o TenantContext no finally; reaplica para o audit.
                TenantContext.setCompanyId(companyId);
            }

            auditor.record(companyId, AuditAction.CREATE, AuditModule.SCHEDULING, "Appointment",
                    appointment.getId().toString(),
                    occurrences.size() > 1
                            ? "Agendamento recorrente criado (" + occurrences.size() + " ocorrências): "
                                    + appointment.getTitle()
                            : "Agendamento criado: " + appointment.getTitle(),
                    createdBy, null);
            return toResponse(appointment);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getById(UUID companyId, UUID appointmentId) {
        try {
            TenantContext.setCompanyId(companyId);
            return toResponse(requireOwned(companyId, appointmentId));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public AppointmentResponse update(UUID companyId, UUID appointmentId, UpdateAppointmentRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            Appointment appointment = requireOwned(companyId, appointmentId);
            validateTags(companyId, request.tagIds());
            validateProcedures(companyId, request.catalogItemIds());
            // A atualização substitui a cadeira (null = sem cadeira): só a nova precisa estar livre.
            checkChairConflicts(companyId, request.chairId(), appointment.getStartAt(),
                    appointment.getEndAt(), appointmentId);
            appointment.update(request.title(), request.contactId(), request.opportunityId(),
                    request.locationKind(), request.locationDetail(), request.notes(), request.chairId());
            appointmentRepository.save(appointment);
            // tagIds ausente mantém os marcadores; uma lista (mesmo vazia) substitui.
            if (request.tagIds() != null) {
                tagRepository.replace(companyId, appointmentId, request.tagIds());
            }
            // Mesma regra para os procedimentos: ausente mantém, lista (mesmo vazia) substitui.
            if (request.catalogItemIds() != null) {
                procedureRepository.replace(companyId, appointmentId, request.catalogItemIds());
            }

            auditor.record(companyId, AuditAction.UPDATE, AuditModule.SCHEDULING, "Appointment",
                    appointment.getId().toString(), "Agendamento atualizado: " + appointment.getTitle(),
                    null, null);
            return toResponse(appointment);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public AppointmentResponse reschedule(UUID companyId, UUID appointmentId, RescheduleRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            Appointment appointment = requireOwned(companyId, appointmentId);
            boolean force = request.force() != null && request.force();
            if (!force) {
                checkConflicts(appointment.getHostId(), request.startAt(), request.endAt(),
                        companyId, appointmentId);
                checkChairConflicts(companyId, appointment.getChairId(), request.startAt(), request.endAt(), appointmentId);
            } else {
                checkHardConflicts(appointment.getHostId(), request.startAt(), request.endAt(),
                        companyId, appointmentId);
                checkChairConflicts(companyId, appointment.getChairId(), request.startAt(), request.endAt(), appointmentId);
            }

            appointment.reschedule(request.startAt(), request.endAt());
            appointmentRepository.save(appointment);

            auditor.record(companyId, AuditAction.UPDATE, AuditModule.SCHEDULING, "Appointment",
                    appointment.getId().toString(), "Agendamento remarcado: " + appointment.getTitle(),
                    null, null);
            return toResponse(appointment);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public AppointmentResponse changeStatus(UUID companyId, UUID appointmentId, AppointmentStatus status) {
        try {
            TenantContext.setCompanyId(companyId);
            Appointment appointment = requireOwned(companyId, appointmentId);
            switch (status) {
                case CONFIRMED -> appointment.confirm();
                case CANCELED -> appointment.cancel(null);
                case COMPLETED -> appointment.complete();
                case NO_SHOW -> appointment.markNoShow();
                case SCHEDULED -> throw new SchedulingValidationException("Use a remarcação para voltar ao status agendado.");
            }
            appointmentRepository.save(appointment);

            auditor.record(companyId, AuditAction.UPDATE, AuditModule.SCHEDULING, "Appointment",
                    appointment.getId().toString(),
                    "Agendamento " + status.name().toLowerCase() + ": " + appointment.getTitle(),
                    null, null);
            return toResponse(appointment);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public AppointmentResponse cancel(UUID companyId, UUID appointmentId, String reason) {
        try {
            TenantContext.setCompanyId(companyId);
            Appointment appointment = requireOwned(companyId, appointmentId);
            String trimmed = reason == null || reason.isBlank() ? null : reason.trim();
            if (trimmed != null && trimmed.length() > 500) {
                trimmed = trimmed.substring(0, 500);
            }
            appointment.cancel(trimmed);
            appointmentRepository.save(appointment);

            auditor.record(companyId, AuditAction.UPDATE, AuditModule.SCHEDULING, "Appointment",
                    appointment.getId().toString(), "Agendamento cancelado: " + appointment.getTitle(),
                    null, null);
            return toResponse(appointment);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void delete(UUID companyId, UUID appointmentId) {
        try {
            TenantContext.setCompanyId(companyId);
            Appointment appointment = requireOwned(companyId, appointmentId);
            appointmentRepository.delete(appointment);

            auditor.record(companyId, AuditAction.DELETE, AuditModule.SCHEDULING, "Appointment",
                    appointmentId.toString(), "Agendamento excluído: " + appointment.getTitle(),
                    null, null);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> list(UUID companyId, Instant from, Instant to, List<UUID> hostIds) {
        try {
            TenantContext.setCompanyId(companyId);
            validateRange(from, to);
            return toResponses(appointmentRepository.findByCompanyIdAndRange(companyId, from, to, hostIds));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> listByContact(UUID companyId, UUID contactId) {
        try {
            TenantContext.setCompanyId(companyId);
            return toResponses(appointmentRepository
                    .findByCompanyIdAndContactIdOrderByStartAtDesc(companyId, contactId, CONTACT_HISTORY_LIMIT));
        } finally {
            TenantContext.clear();
        }
    }

    private void checkOccurrence(CreateAppointmentRequest request, UUID companyId, boolean force,
                                 Instant start, Instant end) {
        if (!force) {
            checkConflicts(request.hostId(), start, end, companyId, null);
        } else {
            checkHardConflicts(request.hostId(), start, end, companyId, null);
        }
        checkChairConflicts(companyId, request.chairId(), start, end, null);
    }

    private static void applyInitialStatus(Appointment appointment, AppointmentStatus status) {
        if (status == null) {
            return;
        }
        switch (status) {
            case CONFIRMED -> appointment.confirm();
            case CANCELED -> appointment.cancel(null);
            case SCHEDULED -> { }
            default -> throw new SchedulingValidationException(
                    "Status inicial inválido: use agendado, confirmado ou cancelado.");
        }
    }

    private static String conflictMessage(List<Instant> conflicts) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(RecurrenceExpander.ZONE);
        String dates = conflicts.stream().limit(5).map(fmt::format).collect(Collectors.joining(", "));
        if (conflicts.size() > 5) {
            dates += " e mais " + (conflicts.size() - 5);
        }
        return "Há conflito de horário em " + conflicts.size()
                + (conflicts.size() == 1 ? " ocorrência: " : " ocorrências: ") + dates + ".";
    }

    private void checkConflicts(UUID hostId, Instant start, Instant end, UUID companyId, UUID excludeId) {
        checkHardConflicts(hostId, start, end, companyId, excludeId);
    }

    private void checkHardConflicts(UUID hostId, Instant start, Instant end, UUID companyId, UUID excludeId) {
        Interval proposed = new Interval(start, end);
        List<Appointment> existing = appointmentRepository.findNonCanceledByHostAndRange(hostId, start, end);
        boolean hasConflict = existing.stream()
                .filter(a -> excludeId == null || !a.getId().equals(excludeId))
                .anyMatch(a -> proposed.overlaps(new Interval(a.getStartAt(), a.getEndAt())));
        if (hasConflict) {
            throw new SlotUnavailableException();
        }

        List<ScheduleBlock> blocks = blockRepository.findByHostIdAndRange(companyId, hostId, start, end);
        boolean hasBlockConflict = blocks.stream()
                .anyMatch(b -> proposed.overlaps(new Interval(b.getStartAt(), b.getEndAt())));
        if (hasBlockConflict) {
            throw new SlotUnavailableException();
        }
    }

    private void checkChairConflicts(UUID companyId, UUID chairId, Instant start, Instant end, UUID excludeAppointmentId) {
        if (chairId == null) {
            return;
        }
        boolean chairExists = chairUseCase.existsInCompany(companyId, chairId);
        // O outro use case limpa o TenantContext no finally; sem reaplicar, os próximos
        // comandos SQL desta transação rodariam sem empresa e o RLS os bloquearia.
        TenantContext.setCompanyId(companyId);
        if (!chairExists) {
            throw new SchedulingValidationException("Cadeira inválida para esta empresa.");
        }
        Interval proposed = new Interval(start, end);
        List<Appointment> existing = appointmentRepository.findByCompanyIdAndRange(companyId, start, end, null);
        boolean hasConflict = existing.stream()
                .filter(a -> a.getChairId() != null)
                .filter(a -> a.getChairId().equals(chairId))
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELED)
                .filter(a -> excludeAppointmentId == null || !a.getId().equals(excludeAppointmentId))
                .anyMatch(a -> proposed.overlaps(new Interval(a.getStartAt(), a.getEndAt())));
        if (hasConflict) {
            throw new SlotUnavailableException();
        }
    }

    private void validateRange(Instant from, Instant to) {
        if (from == null || to == null) {
            throw new SchedulingValidationException("Período de consulta é obrigatório.");
        }
        if (Duration.between(from, to).compareTo(MAX_RANGE) > 0) {
            throw new SchedulingValidationException("O período de consulta não pode exceder 62 dias.");
        }
    }

    private Appointment requireOwned(UUID companyId, UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new SchedulingNotFoundException("Agendamento", appointmentId));
        if (!appointment.getCompanyId().equals(companyId)) {
            throw new SchedulingNotFoundException("Agendamento", appointmentId);
        }
        return appointment;
    }

    private void validateTags(UUID companyId, Collection<UUID> tagIds) {
        if (tagIds == null) {
            return;
        }
        for (UUID tagId : tagIds) {
            boolean valid = tagId != null && tagUseCase.existsInCompany(companyId, tagId);
            TenantContext.setCompanyId(companyId); // ver checkChairConflicts
            if (!valid) {
                throw new SchedulingValidationException("Marcador inválido para esta empresa.");
            }
        }
    }

    private void validateProcedures(UUID companyId, Collection<UUID> catalogItemIds) {
        if (catalogItemIds == null) {
            return;
        }
        for (UUID itemId : catalogItemIds) {
            CatalogItem item = itemId == null ? null : catalogItemRepository.findById(itemId).orElse(null);
            boolean valid = item != null
                    && companyId.equals(item.getCompanyId())
                    && item.getType() == CatalogItemType.SERVICE
                    && item.isActive();
            if (!valid) {
                throw new SchedulingValidationException("Procedimento inválido para o agendamento.");
            }
        }
    }

    private AppointmentResponse toResponse(Appointment a) {
        List<UUID> tagIds = tagRepository.findTagIds(List.of(a.getId())).getOrDefault(a.getId(), List.of());
        List<ProcedureRef> procedures = procedureRepository.findProcedures(List.of(a.getId()))
                .getOrDefault(a.getId(), List.of());
        return toResponse(a, tagIds, procedures);
    }

    /** Lista com marcadores e procedimentos carregados em uma consulta cada (sem N+1). */
    private List<AppointmentResponse> toResponses(List<Appointment> appointments) {
        List<UUID> ids = appointments.stream().map(Appointment::getId).toList();
        Map<UUID, List<UUID>> tags = tagRepository.findTagIds(ids);
        Map<UUID, List<ProcedureRef>> procedures = procedureRepository.findProcedures(ids);
        return appointments.stream()
                .map(a -> toResponse(a, tags.getOrDefault(a.getId(), List.of()),
                        procedures.getOrDefault(a.getId(), List.of())))
                .toList();
    }

    private static AppointmentResponse toResponse(Appointment a, List<UUID> tagIds, List<ProcedureRef> procedures) {
        return new AppointmentResponse(a.getId(), a.getCompanyId(), a.getAppointmentTypeId(),
                a.getHostId(), a.getContactId(), a.getOpportunityId(), a.getTitle(),
                a.getStartAt(), a.getEndAt(), a.getStatus(), a.getSource(), a.getLocationKind(),
                a.getLocationDetail(), a.getMeetingUrl(), a.getNotes(), a.getCancelReason(),
                a.getPublicToken(), a.getCreatedBy(), a.getCreatedAt(), a.getUpdatedAt(), a.getChairId(),
                tagIds, procedures);
    }
}
