package com.becommerce.crm.sales.scheduling.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.sales.scheduling.application.dto.AppointmentTypeResponse;
import com.becommerce.crm.sales.scheduling.application.dto.CreateAppointmentTypeRequest;
import com.becommerce.crm.sales.scheduling.application.dto.UpdateAppointmentTypeRequest;
import com.becommerce.crm.sales.scheduling.application.port.in.AppointmentTypeUseCase;
import com.becommerce.crm.sales.scheduling.application.port.out.AppointmentTypeRepository;
import com.becommerce.crm.sales.scheduling.domain.AppointmentType;
import com.becommerce.crm.sales.scheduling.domain.exception.SchedulingNotFoundException;
import com.becommerce.crm.sales.scheduling.domain.exception.SchedulingValidationException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AppointmentTypeService implements AppointmentTypeUseCase {

    private final AppointmentTypeRepository repository;
    private final TenantAuditRecorder auditor;

    public AppointmentTypeService(AppointmentTypeRepository repository, TenantAuditRecorder auditor) {
        this.repository = repository;
        this.auditor = auditor;
    }

    @Override
    @Transactional
    public AppointmentTypeResponse create(UUID companyId, CreateAppointmentTypeRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            repository.findByCompanyIdAndSlug(companyId, request.slug().trim().toLowerCase())
                    .ifPresent(existing -> {
                        throw new SchedulingValidationException("Já existe um tipo de agendamento com o slug '" + request.slug() + "'.");
                    });

            AppointmentType type = AppointmentType.create(companyId, request.name(), request.slug(),
                    request.description(),
                    request.durationMinutes(),
                    request.bufferBeforeMinutes() != null ? request.bufferBeforeMinutes() : 0,
                    request.bufferAfterMinutes() != null ? request.bufferAfterMinutes() : 0,
                    request.minNoticeHours() != null ? request.minNoticeHours() : 2,
                    request.maxDaysAhead() != null ? request.maxDaysAhead() : 60,
                    request.slotIntervalMinutes() != null ? request.slotIntervalMinutes() : 30,
                    request.color(), request.locationKind(), request.locationDetail(),
                    request.assignmentMode(), request.hostIds());
            repository.save(type);

            auditor.record(companyId, AuditAction.CREATE, AuditModule.SCHEDULING, "AppointmentType",
                    type.getId().toString(), "Tipo de agendamento criado: " + type.getName(), null, null);
            return toResponse(type);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentTypeResponse getById(UUID companyId, UUID typeId) {
        try {
            TenantContext.setCompanyId(companyId);
            return toResponse(requireOwned(companyId, typeId));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public AppointmentTypeResponse update(UUID companyId, UUID typeId, UpdateAppointmentTypeRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            AppointmentType type = requireOwned(companyId, typeId);

            repository.findByCompanyIdAndSlug(companyId, request.slug().trim().toLowerCase())
                    .filter(existing -> !existing.getId().equals(typeId))
                    .ifPresent(existing -> {
                        throw new SchedulingValidationException("Já existe um tipo de agendamento com o slug '" + request.slug() + "'.");
                    });

            type.update(request.name(), request.slug(), request.description(),
                    request.durationMinutes(),
                    request.bufferBeforeMinutes() != null ? request.bufferBeforeMinutes() : 0,
                    request.bufferAfterMinutes() != null ? request.bufferAfterMinutes() : 0,
                    request.minNoticeHours() != null ? request.minNoticeHours() : 2,
                    request.maxDaysAhead() != null ? request.maxDaysAhead() : 60,
                    request.slotIntervalMinutes() != null ? request.slotIntervalMinutes() : 30,
                    request.color(), request.locationKind(), request.locationDetail(),
                    request.assignmentMode(), request.publicBookingEnabled(),
                    request.active() != null ? request.active() : type.isActive(),
                    request.hostIds());
            repository.save(type);

            auditor.record(companyId, AuditAction.UPDATE, AuditModule.SCHEDULING, "AppointmentType",
                    type.getId().toString(), "Tipo de agendamento atualizado: " + type.getName(), null, null);
            return toResponse(type);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void delete(UUID companyId, UUID typeId) {
        try {
            TenantContext.setCompanyId(companyId);
            AppointmentType type = requireOwned(companyId, typeId);
            repository.delete(type);

            auditor.record(companyId, AuditAction.DELETE, AuditModule.SCHEDULING, "AppointmentType",
                    typeId.toString(), "Tipo de agendamento excluído: " + type.getName(), null, null);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentTypeResponse> listByCompany(UUID companyId) {
        try {
            TenantContext.setCompanyId(companyId);
            return repository.findByCompanyId(companyId).stream()
                    .map(AppointmentTypeService::toResponse).toList();
        } finally {
            TenantContext.clear();
        }
    }

    private AppointmentType requireOwned(UUID companyId, UUID typeId) {
        AppointmentType type = repository.findById(typeId)
                .orElseThrow(() -> new SchedulingNotFoundException("Tipo de agendamento", typeId));
        if (!type.getCompanyId().equals(companyId)) {
            throw new SchedulingNotFoundException("Tipo de agendamento", typeId);
        }
        return type;
    }

    private static AppointmentTypeResponse toResponse(AppointmentType t) {
        return new AppointmentTypeResponse(t.getId(), t.getCompanyId(), t.getName(), t.getSlug(),
                t.getDescription(), t.getDurationMinutes(), t.getBufferBeforeMinutes(),
                t.getBufferAfterMinutes(), t.getMinNoticeHours(), t.getMaxDaysAhead(),
                t.getSlotIntervalMinutes(), t.getColor(), t.getLocationKind(), t.getLocationDetail(),
                t.getAssignmentMode(), t.isPublicBookingEnabled(), t.isActive(), t.getHostIds(),
                t.getCreatedAt(), t.getUpdatedAt());
    }
}
