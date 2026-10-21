package com.becommerce.crm.sales.scheduling.application.service;

import com.becommerce.crm.sales.scheduling.application.dto.AgendaSettingsDto;
import com.becommerce.crm.sales.scheduling.application.port.in.AgendaSettingsUseCase;
import com.becommerce.crm.sales.scheduling.infrastructure.persistence.AgendaSettingsJpaEntity;
import com.becommerce.crm.sales.scheduling.infrastructure.persistence.AgendaSettingsJpaRepository;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AgendaSettingsService implements AgendaSettingsUseCase {

    private final AgendaSettingsJpaRepository repository;

    public AgendaSettingsService(AgendaSettingsJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public AgendaSettingsDto get(UUID companyId) {
        try {
            TenantContext.setCompanyId(companyId);
            return repository.findById(companyId)
                    .map(AgendaSettingsService::toDto)
                    .orElseGet(AgendaSettingsDto::defaults);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public AgendaSettingsDto save(UUID companyId, AgendaSettingsDto request) {
        try {
            TenantContext.setCompanyId(companyId);
            AgendaSettingsJpaEntity entity = repository.findById(companyId)
                    .orElseGet(AgendaSettingsJpaEntity::new);
            entity.setCompanyId(companyId);
            entity.setDefaultDurationMinutes(request.defaultDurationMinutes());
            entity.setWorkHoursStart(request.workHoursStart());
            entity.setWorkHoursEnd(request.workHoursEnd());
            entity.setNotifyOnBooking(request.notifyOnBooking());
            entity.setNotify24hBefore(request.notify24hBefore());
            entity.setNotify1hBefore(request.notify1hBefore());
            entity.setNotifyOnCancel(request.notifyOnCancel());
            entity.setNotifyOnReschedule(request.notifyOnReschedule());
            entity.setUpdatedAt(Instant.now());
            repository.save(entity);
            return toDto(entity);
        } finally {
            TenantContext.clear();
        }
    }

    private static AgendaSettingsDto toDto(AgendaSettingsJpaEntity e) {
        return new AgendaSettingsDto(e.getDefaultDurationMinutes(), e.getWorkHoursStart(),
                e.getWorkHoursEnd(), e.isNotifyOnBooking(), e.isNotify24hBefore(),
                e.isNotify1hBefore(), e.isNotifyOnCancel(), e.isNotifyOnReschedule());
    }
}
