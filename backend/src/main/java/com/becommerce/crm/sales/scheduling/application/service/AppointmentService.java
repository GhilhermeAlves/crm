package com.becommerce.crm.sales.scheduling.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.sales.scheduling.application.dto.*;
import com.becommerce.crm.sales.scheduling.application.port.in.AppointmentUseCase;
import com.becommerce.crm.sales.scheduling.application.port.out.AppointmentRepository;
import com.becommerce.crm.sales.scheduling.application.port.out.ScheduleBlockRepository;
import com.becommerce.crm.sales.scheduling.domain.*;
import com.becommerce.crm.sales.scheduling.domain.exception.SchedulingNotFoundException;
import com.becommerce.crm.sales.scheduling.domain.exception.SchedulingValidationException;
import com.becommerce.crm.sales.scheduling.domain.exception.SlotUnavailableException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AppointmentService implements AppointmentUseCase {

    private static final Duration MAX_RANGE = Duration.ofDays(62);

    private final AppointmentRepository appointmentRepository;
    private final ScheduleBlockRepository blockRepository;
    private final TenantAuditRecorder auditor;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              ScheduleBlockRepository blockRepository,
                              TenantAuditRecorder auditor) {
        this.appointmentRepository = appointmentRepository;
        this.blockRepository = blockRepository;
        this.auditor = auditor;
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
            boolean force = request.force() != null && request.force();
            if (!force) {
                checkConflicts(request.hostId(), request.startAt(), request.endAt(), companyId, null);
            } else {
                checkHardConflicts(request.hostId(), request.startAt(), request.endAt(), companyId, null);
            }

            Appointment appointment = Appointment.create(companyId, request.appointmentTypeId(),
                    request.hostId(), request.contactId(), request.opportunityId(),
                    request.title(), request.startAt(), request.endAt(),
                    source, request.locationKind(), request.locationDetail(),
                    request.notes(), createdBy);
            appointmentRepository.save(appointment);

            auditor.record(companyId, AuditAction.CREATE, AuditModule.SCHEDULING, "Appointment",
                    appointment.getId().toString(), "Agendamento criado: " + appointment.getTitle(),
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
            appointment.update(request.title(), request.contactId(), request.opportunityId(),
                    request.locationKind(), request.locationDetail(), request.notes());
            appointmentRepository.save(appointment);

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
            } else {
                checkHardConflicts(appointment.getHostId(), request.startAt(), request.endAt(),
                        companyId, appointmentId);
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
            return appointmentRepository.findByCompanyIdAndRange(companyId, from, to, hostIds)
                    .stream().map(AppointmentService::toResponse).toList();
        } finally {
            TenantContext.clear();
        }
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

    private static AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(a.getId(), a.getCompanyId(), a.getAppointmentTypeId(),
                a.getHostId(), a.getContactId(), a.getOpportunityId(), a.getTitle(),
                a.getStartAt(), a.getEndAt(), a.getStatus(), a.getSource(), a.getLocationKind(),
                a.getLocationDetail(), a.getMeetingUrl(), a.getNotes(), a.getCancelReason(),
                a.getPublicToken(), a.getCreatedBy(), a.getCreatedAt(), a.getUpdatedAt());
    }
}
