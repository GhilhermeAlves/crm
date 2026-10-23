package com.becommerce.crm.sales.scheduling.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.sales.scheduling.application.dto.AppointmentResponse;
import com.becommerce.crm.sales.scheduling.application.port.out.AppointmentRepository;
import com.becommerce.crm.sales.scheduling.application.port.out.ScheduleBlockRepository;
import com.becommerce.crm.sales.scheduling.domain.Appointment;
import com.becommerce.crm.sales.scheduling.domain.AppointmentSource;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppointmentServiceListByContactTest {

    private final AppointmentRepository repository = mock(AppointmentRepository.class);
    private final AppointmentService service = new AppointmentService(repository,
            mock(ScheduleBlockRepository.class), mock(TenantAuditRecorder.class),
            mock(com.becommerce.crm.sales.chair.application.port.in.ChairUseCase.class),
            mock(com.becommerce.crm.sales.tag.application.port.in.TagUseCase.class),
            mock(com.becommerce.crm.sales.scheduling.application.port.out.AppointmentTagRepository.class),
            mock(com.becommerce.crm.masterdata.catalog.application.port.output.CatalogItemRepository.class),
            mock(com.becommerce.crm.sales.scheduling.application.port.out.AppointmentProcedureRepository.class));
    private final UUID companyId = UUID.randomUUID();
    private final UUID contactId = UUID.randomUUID();

    private Appointment appointment(String title, Instant start) {
        return Appointment.create(companyId, null, UUID.randomUUID(), contactId, null,
                title, start, start.plus(1, ChronoUnit.HOURS), AppointmentSource.INTERNAL,
                null, null, null, UUID.randomUUID());
    }

    @Test
    void listsAppointmentsOfContactWithLimit200() {
        Instant later = Instant.now().plus(10, ChronoUnit.DAYS);
        Instant earlier = Instant.now().plus(1, ChronoUnit.DAYS);
        when(repository.findByCompanyIdAndContactIdOrderByStartAtDesc(companyId, contactId, 200))
                .thenReturn(List.of(appointment("Retorno", later), appointment("Avaliação", earlier)));

        List<AppointmentResponse> result = service.listByContact(companyId, contactId);

        assertEquals(2, result.size());
        assertEquals(contactId, result.get(0).contactId());
        verify(repository).findByCompanyIdAndContactIdOrderByStartAtDesc(companyId, contactId, 200);
    }

    @Test
    void returnsEmptyListWhenContactHasNoAppointments() {
        when(repository.findByCompanyIdAndContactIdOrderByStartAtDesc(companyId, contactId, 200))
                .thenReturn(List.of());

        List<AppointmentResponse> result = service.listByContact(companyId, contactId);

        assertEquals(0, result.size());
    }
}
