package com.becommerce.crm.sales.holiday.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.sales.holiday.application.dto.CreateHolidayRequest;
import com.becommerce.crm.sales.holiday.application.dto.HolidayResponse;
import com.becommerce.crm.sales.holiday.application.dto.UpdateHolidayRequest;
import com.becommerce.crm.sales.holiday.application.port.in.HolidayUseCase;
import com.becommerce.crm.sales.holiday.application.port.out.HolidayRepository;
import com.becommerce.crm.sales.holiday.domain.CompanyHoliday;
import com.becommerce.crm.sales.holiday.domain.exception.HolidayNotFoundException;
import com.becommerce.crm.sales.holiday.domain.exception.HolidayValidationException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class HolidayService implements HolidayUseCase {

    private final HolidayRepository repository;
    private final TenantAuditRecorder auditor;

    public HolidayService(HolidayRepository repository, TenantAuditRecorder auditor) {
        this.repository = repository;
        this.auditor = auditor;
    }

    @Override
    @Transactional
    public HolidayResponse create(UUID companyId, CreateHolidayRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            repository.findByCompanyIdAndDate(companyId, request.date())
                    .ifPresent(existing -> {
                        throw new HolidayValidationException("Já existe um feriado cadastrado para esta data.");
                    });
            CompanyHoliday holiday = CompanyHoliday.create(companyId, request.date(), request.name(),
                    request.recurringYearly(), request.blocksSchedule());
            repository.save(holiday);
            auditor.record(companyId, AuditAction.CREATE, AuditModule.SCHEDULING, "CompanyHoliday",
                    holiday.getId().toString(), "Feriado da clínica criado: " + holiday.getName(), null, null);
            return toResponse(holiday);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public HolidayResponse getById(UUID companyId, UUID holidayId) {
        try {
            TenantContext.setCompanyId(companyId);
            return toResponse(requireOwned(companyId, holidayId));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public HolidayResponse update(UUID companyId, UUID holidayId, UpdateHolidayRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            CompanyHoliday holiday = requireOwned(companyId, holidayId);
            if (request.date() != null && !request.date().equals(holiday.getDate())) {
                repository.findByCompanyIdAndDate(companyId, request.date())
                        .filter(existing -> !existing.getId().equals(holidayId))
                        .ifPresent(existing -> {
                            throw new HolidayValidationException("Já existe um feriado cadastrado para esta data.");
                        });
            }
            holiday.update(request.date(), request.name(), request.recurringYearly(), request.blocksSchedule());
            repository.save(holiday);
            auditor.record(companyId, AuditAction.UPDATE, AuditModule.SCHEDULING, "CompanyHoliday",
                    holiday.getId().toString(), "Feriado da clínica atualizado: " + holiday.getName(), null, null);
            return toResponse(holiday);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void delete(UUID companyId, UUID holidayId) {
        try {
            TenantContext.setCompanyId(companyId);
            CompanyHoliday holiday = requireOwned(companyId, holidayId);
            repository.delete(holiday);
            auditor.record(companyId, AuditAction.DELETE, AuditModule.SCHEDULING, "CompanyHoliday",
                    holidayId.toString(), "Feriado da clínica excluído: " + holiday.getName(), null, null);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<HolidayResponse> listByCompany(UUID companyId) {
        try {
            TenantContext.setCompanyId(companyId);
            return repository.findByCompanyId(companyId).stream().map(HolidayService::toResponse).toList();
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<HolidayResponse> listByRange(UUID companyId, LocalDate from, LocalDate to) {
        try {
            TenantContext.setCompanyId(companyId);
            if (from == null || to == null) {
                return List.of();
            }
            LocalDate start = from.isBefore(to) ? from : to;
            LocalDate end = from.isBefore(to) ? to : from;
            return repository.findByCompanyIdAndDateRange(companyId, start, end).stream()
                    .map(HolidayService::toResponse)
                    .toList();
        } finally {
            TenantContext.clear();
        }
    }

    private CompanyHoliday requireOwned(UUID companyId, UUID holidayId) {
        CompanyHoliday holiday = repository.findById(holidayId)
                .orElseThrow(() -> new HolidayNotFoundException("Feriado da clínica", holidayId));
        if (!holiday.getCompanyId().equals(companyId)) {
            throw new HolidayNotFoundException("Feriado da clínica", holidayId);
        }
        return holiday;
    }

    private static HolidayResponse toResponse(CompanyHoliday holiday) {
        return new HolidayResponse(holiday.getId(), holiday.getCompanyId(), holiday.getDate(), holiday.getName(),
                holiday.isRecurringYearly(), holiday.isBlocksSchedule(), holiday.getCreatedAt(),
                holiday.getUpdatedAt());
    }
}
