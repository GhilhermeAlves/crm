package com.becommerce.crm.sales.scheduling.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.sales.chair.application.port.in.ChairUseCase;
import com.becommerce.crm.sales.scheduling.application.dto.AppointmentResponse;
import com.becommerce.crm.sales.scheduling.application.dto.CreateAppointmentRequest;
import com.becommerce.crm.sales.scheduling.application.port.out.AppointmentRepository;
import com.becommerce.crm.sales.scheduling.application.port.out.AppointmentTagRepository;
import com.becommerce.crm.sales.scheduling.application.port.out.ScheduleBlockRepository;
import com.becommerce.crm.sales.scheduling.domain.Appointment;
import com.becommerce.crm.sales.scheduling.domain.AppointmentSource;
import com.becommerce.crm.sales.scheduling.domain.exception.SchedulingValidationException;
import com.becommerce.crm.sales.scheduling.domain.exception.SlotUnavailableException;
import com.becommerce.crm.sales.tag.application.port.in.TagUseCase;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AppointmentServiceChairTagTest {

    private final AppointmentRepository repository = mock(AppointmentRepository.class);
    private final ChairUseCase chairs = mock(ChairUseCase.class);
    private final TagUseCase tags = mock(TagUseCase.class);
    private final AppointmentTagRepository tagRepository = mock(AppointmentTagRepository.class);
    private final AppointmentService service = new AppointmentService(repository,
            mock(ScheduleBlockRepository.class), mock(TenantAuditRecorder.class), chairs, tags, tagRepository,
            mock(com.becommerce.crm.masterdata.catalog.application.port.output.CatalogItemRepository.class),
            mock(com.becommerce.crm.sales.scheduling.application.port.out.AppointmentProcedureRepository.class));

    private final UUID companyId = UUID.randomUUID();
    private final UUID hostId = UUID.randomUUID();
    private final UUID chairId = UUID.randomUUID();
    private final Instant start = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    private CreateAppointmentRequest request(UUID chair, List<UUID> tagIds) {
        return new CreateAppointmentRequest(null, hostId, null, null, "Avaliação", start,
                start.plus(1, ChronoUnit.HOURS), null, null, null, false, chair, tagIds, null, null, null);
    }

    private Appointment existingInChair(UUID chair) {
        return Appointment.create(companyId, null, UUID.randomUUID(), null, null, "Outra consulta",
                start.plus(30, ChronoUnit.MINUTES), start.plus(90, ChronoUnit.MINUTES),
                AppointmentSource.INTERNAL, null, null, null, null, chair);
    }

    @Test
    void rejectsOverlapInSameChair() {
        when(chairs.existsInCompany(companyId, chairId)).thenReturn(true);
        when(repository.findByCompanyIdAndRange(eq(companyId), any(), any(), isNull()))
                .thenReturn(List.of(existingInChair(chairId)));

        assertThrows(SlotUnavailableException.class, () -> service.create(companyId, request(chairId, null), null));
        verify(repository, never()).save(any());
    }

    @Test
    void allowsOverlapInAnotherChair() {
        when(chairs.existsInCompany(companyId, chairId)).thenReturn(true);
        when(repository.findByCompanyIdAndRange(eq(companyId), any(), any(), isNull()))
                .thenReturn(List.of(existingInChair(UUID.randomUUID())));

        AppointmentResponse response = service.create(companyId, request(chairId, null), null);

        assertEquals(chairId, response.chairId());
    }

    @Test
    void rejectsChairFromAnotherCompany() {
        when(chairs.existsInCompany(companyId, chairId)).thenReturn(false);

        assertThrows(SchedulingValidationException.class,
                () -> service.create(companyId, request(chairId, null), null));
    }

    @Test
    void rejectsTagFromAnotherCompany() {
        UUID foreignTag = UUID.randomUUID();
        when(tags.existsInCompany(companyId, foreignTag)).thenReturn(false);

        assertThrows(SchedulingValidationException.class,
                () -> service.create(companyId, request(null, List.of(foreignTag)), null));
        verify(repository, never()).save(any());
    }

    @Test
    void savesTagsAndKeepsTenantAfterCallingOtherUseCases() {
        UUID tagId = UUID.randomUUID();
        // Os use cases de cadeira e marcador limpam o TenantContext no finally (como os reais).
        when(chairs.existsInCompany(companyId, chairId)).thenAnswer(inv -> {
            TenantContext.clear();
            return true;
        });
        when(tags.existsInCompany(companyId, tagId)).thenAnswer(inv -> {
            TenantContext.clear();
            return true;
        });
        AtomicReference<UUID> tenantOnSave = new AtomicReference<>();
        when(repository.save(any())).thenAnswer(inv -> {
            tenantOnSave.set(TenantContext.getCompanyId());
            return inv.getArgument(0);
        });
        when(tagRepository.findTagIds(any())).thenAnswer(inv -> {
            List<UUID> ids = List.copyOf(inv.getArgument(0));
            return Map.of(ids.get(0), List.of(tagId));
        });

        AppointmentResponse response = service.create(companyId, request(chairId, List.of(tagId)), null);

        assertEquals(companyId, tenantOnSave.get(), "o save precisa rodar com a empresa no contexto (RLS)");
        verify(tagRepository).replace(companyId, response.id(), List.of(tagId));
        assertEquals(List.of(tagId), response.tagIds());
    }

    @Test
    void listLoadsTagsInOneQuery() {
        Appointment a = existingInChair(null);
        Appointment b = existingInChair(null);
        UUID tagId = UUID.randomUUID();
        when(repository.findByCompanyIdAndRange(eq(companyId), any(), any(), any())).thenReturn(List.of(a, b));
        when(tagRepository.findTagIds(any())).thenReturn(Map.of(a.getId(), List.of(tagId)));

        List<AppointmentResponse> result = service.list(companyId, start, start.plus(1, ChronoUnit.DAYS), null);

        verify(tagRepository, times(1)).findTagIds(List.of(a.getId(), b.getId()));
        assertEquals(List.of(tagId), result.get(0).tagIds());
        assertEquals(List.of(), result.get(1).tagIds());
    }

    @Test
    void marksContactAsPatientOnCreateAndRestoresTenant() {
        UUID contactId = UUID.randomUUID();
        com.becommerce.crm.masterdata.contact.application.service.PatientAutoMarker marker =
                mock(com.becommerce.crm.masterdata.contact.application.service.PatientAutoMarker.class);
        AtomicReference<UUID> tenantAtAudit = new AtomicReference<>();
        when(marker.markOnAppointment(companyId, contactId)).thenAnswer(inv -> {
            TenantContext.clear(); // como o ContactService faz no finally
            return true;
        });
        TenantAuditRecorder auditor = mock(TenantAuditRecorder.class);
        doAnswer(inv -> {
            tenantAtAudit.set(TenantContext.getCompanyId());
            return null;
        }).when(auditor).record(any(), any(), any(), any(), any(), any(), any(), any());
        AppointmentService withMarker = new AppointmentService(repository,
                mock(ScheduleBlockRepository.class), auditor, chairs, tags, tagRepository,
            mock(com.becommerce.crm.masterdata.catalog.application.port.output.CatalogItemRepository.class),
            mock(com.becommerce.crm.sales.scheduling.application.port.out.AppointmentProcedureRepository.class));
        withMarker.setPatientAutoMarker(marker);

        withMarker.create(companyId, new CreateAppointmentRequest(null, hostId, contactId, null, "Avaliação",
                start, start.plus(1, ChronoUnit.HOURS), null, null, null, false, null, null, null, null, null), null);

        verify(marker).markOnAppointment(companyId, contactId);
        assertEquals(companyId, tenantAtAudit.get());
    }
}
