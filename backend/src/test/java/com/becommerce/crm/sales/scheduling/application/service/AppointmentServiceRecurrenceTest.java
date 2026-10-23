package com.becommerce.crm.sales.scheduling.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.sales.chair.application.port.in.ChairUseCase;
import com.becommerce.crm.sales.scheduling.application.dto.*;
import com.becommerce.crm.sales.scheduling.application.port.out.AppointmentRepository;
import com.becommerce.crm.sales.scheduling.application.port.out.AppointmentTagRepository;
import com.becommerce.crm.sales.scheduling.application.port.out.ScheduleBlockRepository;
import com.becommerce.crm.sales.scheduling.domain.Appointment;
import com.becommerce.crm.sales.scheduling.domain.AppointmentSource;
import com.becommerce.crm.sales.scheduling.domain.AppointmentStatus;
import com.becommerce.crm.sales.scheduling.domain.exception.SlotUnavailableException;
import com.becommerce.crm.sales.tag.application.port.in.TagUseCase;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AppointmentServiceRecurrenceTest {

    private final AppointmentRepository repository = mock(AppointmentRepository.class);
    private final AppointmentTagRepository tagRepository = mock(AppointmentTagRepository.class);
    private final TagUseCase tags = mock(TagUseCase.class);
    private final AppointmentService service = new AppointmentService(repository,
            mock(ScheduleBlockRepository.class), mock(TenantAuditRecorder.class), mock(ChairUseCase.class),
            tags, tagRepository);

    private final UUID companyId = UUID.randomUUID();
    private final UUID hostId = UUID.randomUUID();
    private final Instant start = Instant.parse("2030-03-04T12:00:00Z");

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    private CreateAppointmentRequest request(AppointmentStatus status, RecurrenceRequest recurrence, List<UUID> tagIds) {
        return new CreateAppointmentRequest(null, hostId, null, null, "Avaliação", start,
                start.plus(1, ChronoUnit.HOURS), null, null, null, false, null, tagIds, status, recurrence);
    }

    @Test
    void appliesConfirmedStatus() {
        AppointmentResponse r = service.create(companyId, request(AppointmentStatus.CONFIRMED, null, null), null);
        assertEquals(AppointmentStatus.CONFIRMED, r.status());
    }

    @Test
    void appliesCanceledStatus() {
        AppointmentResponse r = service.create(companyId, request(AppointmentStatus.CANCELED, null, null), null);
        assertEquals(AppointmentStatus.CANCELED, r.status());
    }

    @Test
    void recurrenceCreatesAllWithSameGroup() {
        UUID tag = UUID.randomUUID();
        when(tags.existsInCompany(companyId, tag)).thenReturn(true);
        RecurrenceRequest rec = new RecurrenceRequest(RecurrenceFrequency.WEEKLY, 1, RecurrenceEnd.AFTER_COUNT, null, 4);

        AppointmentResponse r = service.create(companyId, request(null, rec, List.of(tag)), null);

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(repository, times(4)).save(captor.capture());
        List<Appointment> saved = captor.getAllValues();
        assertNotNull(saved.get(0).getRecurrenceGroupId());
        assertTrue(saved.stream().allMatch(a -> saved.get(0).getRecurrenceGroupId().equals(a.getRecurrenceGroupId())));
        assertEquals(start, r.startAt());
        assertEquals(start.plus(21, ChronoUnit.DAYS), saved.get(3).getStartAt());
        verify(tagRepository, times(4)).replace(eq(companyId), any(), eq(List.of(tag)));
    }

    @Test
    void conflictInOneOccurrenceSavesNothing() {
        Instant third = start.plus(2, ChronoUnit.DAYS);
        Appointment busy = Appointment.create(companyId, null, hostId, null, null, "Outro",
                third, third.plus(1, ChronoUnit.HOURS), AppointmentSource.INTERNAL, null, null, null, null);
        when(repository.findNonCanceledByHostAndRange(eq(hostId), any(), any()))
                .thenAnswer(inv -> third.equals(inv.getArgument(1)) ? List.of(busy) : List.of());
        RecurrenceRequest rec = new RecurrenceRequest(RecurrenceFrequency.DAILY, 1, RecurrenceEnd.AFTER_COUNT, null, 5);

        SlotUnavailableException ex = assertThrows(SlotUnavailableException.class,
                () -> service.create(companyId, request(null, rec, null), null));
        assertTrue(ex.getMessage().contains("06/03 09:00"), ex.getMessage());
        verify(repository, never()).save(any());
    }
}
