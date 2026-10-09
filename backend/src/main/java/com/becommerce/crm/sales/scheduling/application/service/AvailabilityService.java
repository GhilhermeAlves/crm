package com.becommerce.crm.sales.scheduling.application.service;

import com.becommerce.crm.sales.scheduling.application.dto.*;
import com.becommerce.crm.sales.scheduling.application.port.in.AvailabilityUseCase;
import com.becommerce.crm.sales.scheduling.application.port.out.AvailabilityRepository;
import com.becommerce.crm.sales.scheduling.domain.AvailabilityOverride;
import com.becommerce.crm.sales.scheduling.domain.AvailabilityWindow;
import com.becommerce.crm.sales.scheduling.domain.TimeRange;
import com.becommerce.crm.sales.scheduling.domain.exception.SchedulingValidationException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

@Service
public class AvailabilityService implements AvailabilityUseCase {

    private static final String DEFAULT_TIMEZONE = "America/Sao_Paulo";

    private final AvailabilityRepository repository;

    public AvailabilityService(AvailabilityRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public AvailabilityResponse get(UUID companyId, UUID userId) {
        try {
            TenantContext.setCompanyId(companyId);
            String tz = repository.findTimezone(userId).orElse(DEFAULT_TIMEZONE);
            List<AvailabilityWindow> windows = repository.findWindowsByUserId(companyId, userId);
            List<AvailabilityOverride> overrides = repository.findOverridesByUserId(companyId, userId);

            return new AvailabilityResponse(tz,
                    windows.stream().map(w -> new AvailabilityRuleDto(w.weekday().getValue(), w.start(), w.end()))
                            .toList(),
                    overrides.stream().map(o -> new AvailabilityOverrideDto(o.date(),
                            o.windows() != null ? o.windows().stream()
                                    .map(r -> new AvailabilityOverrideDto.TimeSlotDto(r.start(), r.end())).toList()
                                    : List.of())).toList());
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public AvailabilityResponse set(UUID companyId, UUID userId, SetAvailabilityRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            String tz = request.timezone() != null ? request.timezone() : DEFAULT_TIMEZONE;
            repository.saveTimezone(userId, companyId, tz);

            if (request.rules() != null) {
                List<AvailabilityWindow> windows = request.rules().stream()
                        .distinct()
                        .map(r -> new AvailabilityWindow(UUID.randomUUID(), companyId, userId,
                                toDayOfWeek(r), r.startTime(), r.endTime()))
                        .toList();
                repository.replaceWindows(companyId, userId, windows);
            }

            if (request.overrides() != null) {
                List<AvailabilityOverride> overrides = request.overrides().stream()
                        .map(o -> new AvailabilityOverride(UUID.randomUUID(), companyId, userId,
                                o.date(),
                                o.windows() != null ? o.windows().stream()
                                        .map(s -> new TimeRange(s.start(), s.end())).toList()
                                        : List.of()))
                        .toList();
                repository.replaceOverrides(companyId, userId, overrides);
            }

            return get(companyId, userId);
        } finally {
            TenantContext.clear();
        }
    }

    private static DayOfWeek toDayOfWeek(AvailabilityRuleDto rule) {
        if (rule.weekday() < 1 || rule.weekday() > 7) {
            throw new SchedulingValidationException("Dia da semana inválido: " + rule.weekday());
        }
        if (rule.startTime() == null || rule.endTime() == null || !rule.endTime().isAfter(rule.startTime())) {
            throw new SchedulingValidationException("O horário final deve ser depois do inicial.");
        }
        return DayOfWeek.of(rule.weekday());
    }
}
