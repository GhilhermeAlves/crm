package com.becommerce.crm.sales.scheduling.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.sales.scheduling.application.dto.AppointmentResponse;
import com.becommerce.crm.sales.scheduling.application.port.out.AppointmentRepository;
import com.becommerce.crm.sales.scheduling.application.port.out.ScheduleBlockRepository;
import com.becommerce.crm.sales.scheduling.domain.Appointment;
import com.becommerce.crm.sales.scheduling.domain.AppointmentSource;
import com.becommerce.crm.sales.scheduling.domain.AppointmentStatus;
import com.becommerce.crm.sales.scheduling.domain.exception.SchedulingNotFoundException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppointmentServiceCancelTest {

    private final AppointmentRepository repository = mock(AppointmentRepository.class);
    private final AppointmentService service = new AppointmentService(repository,
            mock(ScheduleBlockRepository.class), mock(TenantAuditRecorder.class),
            mock(com.becommerce.crm.sales.chair.application.port.in.ChairUseCase.class),
            mock(com.becommerce.crm.sales.tag.application.port.in.TagUseCase.class),
            mock(com.becommerce.crm.sales.scheduling.application.port.out.AppointmentTagRepository.class),
            mock(com.becommerce.crm.masterdata.catalog.application.port.output.CatalogItemRepository.class),
            mock(com.becommerce.crm.sales.scheduling.application.port.out.AppointmentProcedureRepository.class));
    private final UUID companyId = UUID.randomUUID();

    private Appointment appointment(UUID company) {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        Appointment a = Appointment.create(company, null, UUID.randomUUID(), UUID.randomUUID(), null,
                "Avaliação", start, start.plus(1, ChronoUnit.HOURS), AppointmentSource.INTERNAL,
                null, null, null, UUID.randomUUID());
        when(repository.findById(a.getId())).thenReturn(Optional.of(a));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        return a;
    }

    @Test
    void cancelsWithReason() {
        Appointment a = appointment(companyId);

        AppointmentResponse response = service.cancel(companyId, a.getId(), "  Paciente viajou  ");

        assertEquals(AppointmentStatus.CANCELED, response.status());
        assertEquals("Paciente viajou", response.cancelReason());
        verify(repository).save(a);
    }

    @Test
    void blankReasonBecomesNull() {
        Appointment a = appointment(companyId);

        AppointmentResponse response = service.cancel(companyId, a.getId(), "   ");

        assertEquals(AppointmentStatus.CANCELED, response.status());
        assertNull(response.cancelReason());
    }

    @Test
    void rejectsAppointmentFromAnotherCompany() {
        Appointment a = appointment(UUID.randomUUID());

        assertThrows(SchedulingNotFoundException.class, () -> service.cancel(companyId, a.getId(), "x"));
        verify(repository, never()).save(any());
    }
}
