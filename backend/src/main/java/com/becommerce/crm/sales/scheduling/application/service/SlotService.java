package com.becommerce.crm.sales.scheduling.application.service;

import com.becommerce.crm.sales.scheduling.application.dto.SlotResponse;
import com.becommerce.crm.sales.scheduling.application.port.out.AppointmentRepository;
import com.becommerce.crm.sales.scheduling.application.port.out.AppointmentTypeRepository;
import com.becommerce.crm.sales.scheduling.application.port.out.AvailabilityRepository;
import com.becommerce.crm.sales.scheduling.application.port.out.ScheduleBlockRepository;
import com.becommerce.crm.sales.scheduling.domain.*;
import com.becommerce.crm.sales.scheduling.domain.exception.SchedulingNotFoundException;
import com.becommerce.crm.sales.scheduling.domain.exception.SchedulingValidationException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Stream;

@Service
public class SlotService {

    private final AppointmentTypeRepository typeRepository;
    private final AvailabilityRepository availabilityRepository;
    private final AppointmentRepository appointmentRepository;
    private final ScheduleBlockRepository blockRepository;

    public SlotService(AppointmentTypeRepository typeRepository,
                       AvailabilityRepository availabilityRepository,
                       AppointmentRepository appointmentRepository,
                       ScheduleBlockRepository blockRepository) {
        this.typeRepository = typeRepository;
        this.availabilityRepository = availabilityRepository;
        this.appointmentRepository = appointmentRepository;
        this.blockRepository = blockRepository;
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> getAvailableSlots(UUID companyId, UUID typeId, LocalDate from, LocalDate to) {
        try {
            TenantContext.setCompanyId(companyId);

            AppointmentType type = typeRepository.findById(typeId)
                    .orElseThrow(() -> new SchedulingNotFoundException("Tipo de agendamento", typeId));
            if (!type.getCompanyId().equals(companyId)) {
                throw new SchedulingNotFoundException("Tipo de agendamento", typeId);
            }
            if (!type.isActive()) {
                throw new SchedulingValidationException("Este tipo de agendamento está inativo.");
            }

            List<UUID> hostIds = type.getHostIds();
            if (hostIds.isEmpty()) {
                return List.of();
            }

            Set<Interval> allSlots = new TreeSet<>(Comparator.comparing(Interval::start));

            for (UUID hostId : hostIds) {
                ZoneId hostZone = ZoneId.of(
                        availabilityRepository.findTimezone(hostId).orElse("America/Sao_Paulo"));
                List<AvailabilityWindow> windows = availabilityRepository.findWindowsByUserId(companyId, hostId);
                List<AvailabilityOverride> overrides = availabilityRepository
                        .findOverridesByUserIdAndDateRange(companyId, hostId, from, to);
                WeeklyAvailability availability = new WeeklyAvailability(windows, overrides, hostZone);

                Instant rangeStart = from.atStartOfDay(hostZone).toInstant();
                Instant rangeEnd = to.plusDays(1).atStartOfDay(hostZone).toInstant();

                List<Appointment> appointments = appointmentRepository
                        .findNonCanceledByHostAndRange(hostId, rangeStart, rangeEnd);
                List<ScheduleBlock> blocks = blockRepository
                        .findByHostIdAndRange(companyId, hostId, rangeStart, rangeEnd);

                List<Interval> busyIntervals = Stream.concat(
                        appointments.stream().map(a -> new Interval(a.getStartAt(), a.getEndAt())),
                        blocks.stream().map(b -> new Interval(b.getStartAt(), b.getEndAt()))
                ).toList();

                LocalDate day = from;
                while (!day.isAfter(to)) {
                    List<TimeRange> dayWindows = availability.getWindowsForDate(day);
                    List<Interval> slots = SlotCalculator.freeSlots(day, type.getDurationMinutes(),
                            type.getSlotIntervalMinutes(), type.getBufferBeforeMinutes(),
                            type.getBufferAfterMinutes(), type.getMinNoticeHours(),
                            type.getMaxDaysAhead(), hostZone, dayWindows, busyIntervals);
                    allSlots.addAll(slots);
                    day = day.plusDays(1);
                }
            }

            return allSlots.stream()
                    .map(s -> new SlotResponse(s.start(), s.end()))
                    .toList();
        } finally {
            TenantContext.clear();
        }
    }
}
