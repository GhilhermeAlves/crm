package com.becommerce.crm.sales.scheduling.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.masterdata.catalog.application.port.output.CatalogItemRepository;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItem;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItemType;
import com.becommerce.crm.sales.chair.application.port.in.ChairUseCase;
import com.becommerce.crm.sales.scheduling.application.dto.AppointmentResponse;
import com.becommerce.crm.sales.scheduling.application.dto.CreateAppointmentRequest;
import com.becommerce.crm.sales.scheduling.application.dto.ProcedureRef;
import com.becommerce.crm.sales.scheduling.application.dto.RecurrenceEnd;
import com.becommerce.crm.sales.scheduling.application.dto.RecurrenceFrequency;
import com.becommerce.crm.sales.scheduling.application.dto.RecurrenceRequest;
import com.becommerce.crm.sales.scheduling.application.dto.UpdateAppointmentRequest;
import com.becommerce.crm.sales.scheduling.application.port.out.AppointmentProcedureRepository;
import com.becommerce.crm.sales.scheduling.application.port.out.AppointmentRepository;
import com.becommerce.crm.sales.scheduling.application.port.out.AppointmentTagRepository;
import com.becommerce.crm.sales.scheduling.application.port.out.ScheduleBlockRepository;
import com.becommerce.crm.sales.scheduling.domain.Appointment;
import com.becommerce.crm.sales.scheduling.domain.AppointmentSource;
import com.becommerce.crm.sales.scheduling.domain.exception.SchedulingValidationException;
import com.becommerce.crm.sales.tag.application.port.in.TagUseCase;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AppointmentServiceProceduresTest {

    private final AppointmentRepository repository = mock(AppointmentRepository.class);
    private final CatalogItemRepository catalog = mock(CatalogItemRepository.class);
    private final AppointmentProcedureRepository procedures = mock(AppointmentProcedureRepository.class);
    private final AppointmentService service = new AppointmentService(repository,
            mock(ScheduleBlockRepository.class), mock(TenantAuditRecorder.class), mock(ChairUseCase.class),
            mock(TagUseCase.class), mock(AppointmentTagRepository.class), catalog, procedures);

    private final UUID companyId = UUID.randomUUID();
    private final UUID hostId = UUID.randomUUID();
    private final Instant start = Instant.parse("2030-03-04T12:00:00Z");

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    private CatalogItem item(UUID company, CatalogItemType type, boolean active) {
        CatalogItem item = CatalogItem.create(company, type, "Limpeza", null, null, null, "BRL", null, null, null);
        if (!active) {
            item.deactivate();
        }
        when(catalog.findById(item.getId())).thenReturn(Optional.of(item));
        return item;
    }

    private CreateAppointmentRequest request(List<UUID> ids, RecurrenceRequest recurrence) {
        return new CreateAppointmentRequest(null, hostId, null, null, "Avaliação", start,
                start.plus(1, ChronoUnit.HOURS), null, null, null, false, null, null, null, recurrence, ids);
    }

    private Appointment existing() {
        return Appointment.create(companyId, null, hostId, null, null, "Avaliação", start,
                start.plus(1, ChronoUnit.HOURS), AppointmentSource.INTERNAL, null, null, null, null, null);
    }

    @Test
    void createSavesProceduresOfValidServices() {
        CatalogItem a = item(companyId, CatalogItemType.SERVICE, true);
        CatalogItem b = item(companyId, CatalogItemType.SERVICE, true);

        service.create(companyId, request(List.of(a.getId(), b.getId()), null), null);

        verify(procedures).replace(eq(companyId), any(), eq(List.of(a.getId(), b.getId())));
    }

    @Test
    void createAppliesProceduresToEveryOccurrence() {
        CatalogItem a = item(companyId, CatalogItemType.SERVICE, true);
        RecurrenceRequest weekly = new RecurrenceRequest(RecurrenceFrequency.WEEKLY, 1,
                RecurrenceEnd.AFTER_COUNT, null, 3);

        service.create(companyId, request(List.of(a.getId()), weekly), null);

        verify(procedures, times(3)).replace(eq(companyId), any(), eq(List.of(a.getId())));
    }

    @Test
    void createWithoutProceduresDoesNotTouchTable() {
        service.create(companyId, request(null, null), null);

        verify(procedures, never()).replace(any(), any(), any());
    }

    @Test
    void createRejectsProduct() {
        CatalogItem product = item(companyId, CatalogItemType.PRODUCT, true);

        SchedulingValidationException e = assertThrows(SchedulingValidationException.class,
                () -> service.create(companyId, request(List.of(product.getId()), null), null));

        assertEquals("Procedimento inválido para o agendamento.", e.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsInactiveService() {
        CatalogItem inactive = item(companyId, CatalogItemType.SERVICE, false);

        assertThrows(SchedulingValidationException.class,
                () -> service.create(companyId, request(List.of(inactive.getId()), null), null));
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsServiceFromAnotherCompany() {
        CatalogItem other = item(UUID.randomUUID(), CatalogItemType.SERVICE, true);

        assertThrows(SchedulingValidationException.class,
                () -> service.create(companyId, request(List.of(other.getId()), null), null));
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsUnknownItem() {
        UUID unknown = UUID.randomUUID();
        when(catalog.findById(unknown)).thenReturn(Optional.empty());

        assertThrows(SchedulingValidationException.class,
                () -> service.create(companyId, request(List.of(unknown), null), null));
    }

    @Test
    void updateReplacesProcedures() {
        Appointment appointment = existing();
        when(repository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        CatalogItem a = item(companyId, CatalogItemType.SERVICE, true);

        service.update(companyId, appointment.getId(), new UpdateAppointmentRequest("Novo título", null, null,
                null, null, null, null, null, List.of(a.getId())));

        verify(procedures).replace(companyId, appointment.getId(), List.of(a.getId()));
    }

    @Test
    void updateWithEmptyListClearsAndNullKeeps() {
        Appointment appointment = existing();
        when(repository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

        service.update(companyId, appointment.getId(), new UpdateAppointmentRequest(null, null, null,
                null, null, null, null, null, List.of()));
        verify(procedures).replace(companyId, appointment.getId(), List.of());

        clearInvocations(procedures);
        service.update(companyId, appointment.getId(), new UpdateAppointmentRequest(null, null, null,
                null, null, null, null, null, null));
        verify(procedures, never()).replace(any(), any(), any());
    }

    @Test
    void updateRejectsInvalidProcedure() {
        Appointment appointment = existing();
        when(repository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        CatalogItem product = item(companyId, CatalogItemType.PRODUCT, true);

        assertThrows(SchedulingValidationException.class, () -> service.update(companyId, appointment.getId(),
                new UpdateAppointmentRequest(null, null, null, null, null, null, null, null,
                        List.of(product.getId()))));
        verify(procedures, never()).replace(any(), any(), any());
    }

    @Test
    void responsesCarryProceduresFromBatchLookup() {
        Appointment appointment = existing();
        ProcedureRef ref = new ProcedureRef(UUID.randomUUID(), "Limpeza");
        when(repository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(repository.findByCompanyIdAndRange(eq(companyId), any(), any(), any()))
                .thenReturn(List.of(appointment));
        when(procedures.findProcedures(anyCollection())).thenReturn(Map.of(appointment.getId(), List.of(ref)));

        AppointmentResponse detail = service.getById(companyId, appointment.getId());
        List<AppointmentResponse> list = service.list(companyId, start, start.plus(1, ChronoUnit.DAYS), null);

        assertEquals(List.of(ref), detail.procedures());
        assertEquals(List.of(ref), list.get(0).procedures());
        verify(procedures, times(2)).findProcedures(anyCollection());
    }
}
