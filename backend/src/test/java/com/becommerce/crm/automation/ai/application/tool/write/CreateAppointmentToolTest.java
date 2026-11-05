package com.becommerce.crm.automation.ai.application.tool.write;

import com.becommerce.crm.automation.ai.application.action.AiActionExecutor;
import com.becommerce.crm.automation.ai.application.action.AiActionService;
import com.becommerce.crm.automation.ai.application.context.AiPermissionContext;
import com.becommerce.crm.automation.ai.application.tool.AiToolContext;
import com.becommerce.crm.automation.ai.application.tool.SchedulingProfessionals;
import com.becommerce.crm.automation.ai.application.tool.SchedulingProfessionals.Professional;
import com.becommerce.crm.automation.ai.domain.AiAction;
import com.becommerce.crm.finance.transaction.application.service.FinancialTransactionService;
import com.becommerce.crm.masterdata.contact.application.dto.response.ContactResponse;
import com.becommerce.crm.masterdata.contact.application.port.in.ContactUseCase;
import com.becommerce.crm.masterdata.inventory.application.service.InventoryService;
import com.becommerce.crm.sales.activity.application.port.in.ActivityUseCase;
import com.becommerce.crm.sales.pipeline.application.port.in.OpportunityUseCase;
import com.becommerce.crm.sales.scheduling.application.dto.AppointmentResponse;
import com.becommerce.crm.sales.scheduling.application.dto.AppointmentTypeResponse;
import com.becommerce.crm.sales.scheduling.application.dto.CreateAppointmentRequest;
import com.becommerce.crm.sales.scheduling.application.port.in.AppointmentTypeUseCase;
import com.becommerce.crm.sales.scheduling.application.port.in.AppointmentUseCase;
import com.becommerce.crm.sales.scheduling.application.port.in.BlockUseCase;
import com.becommerce.crm.sales.scheduling.domain.AppointmentStatus;
import com.becommerce.crm.sales.task.application.port.in.TaskUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateAppointmentToolTest {

    private final UUID companyId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();
    private final UUID contactId = UUID.randomUUID();
    private final AiToolContext ctx = new AiToolContext(companyId, userId,
            new AiPermissionContext(List.of("appointment:create")), conversationId);

    private final AiActionService actionService = mock(AiActionService.class);
    private final AppointmentUseCase appointments = mock(AppointmentUseCase.class);
    private final AppointmentTypeUseCase types = mock(AppointmentTypeUseCase.class);
    private final BlockUseCase blocks = mock(BlockUseCase.class);
    private final ContactUseCase contacts = mock(ContactUseCase.class);
    private final SchedulingProfessionals professionals = mock(SchedulingProfessionals.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-05T12:00:00Z"), ZoneOffset.UTC);

    private final CreateAppointmentTool tool = new CreateAppointmentTool(actionService, appointments, types,
            blocks, contacts, professionals, clock);

    @BeforeEach
    void setUp() {
        ContactResponse contact = mock(ContactResponse.class);
        when(contact.companyId()).thenReturn(companyId);
        when(contact.firstName()).thenReturn("Lúcia");
        when(contact.lastName()).thenReturn("de Aguiar da Silva");
        when(contacts.getById(companyId, contactId)).thenReturn(contact);
        when(professionals.list(companyId)).thenReturn(List.of(new Professional(userId, "Dra. Ana")));
        when(appointments.list(eq(companyId), any(), any(), any())).thenReturn(List.of());
        when(blocks.list(eq(companyId), any(), any(), any())).thenReturn(List.of());
    }

    @Test
    @SuppressWarnings("unchecked")
    void proposesWithDefaultsAndReadableDescription() {
        tool.execute(ctx, Map.of("contactId", contactId.toString(), "startAt", "2026-11-11T13:30"));

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(actionService).propose(eq(companyId), eq(userId), eq(conversationId), eq("create_appointment"),
                eq("CONTACT"), eq(contactId), params.capture(),
                eq("Marcar consulta: Consulta - Lúcia de Aguiar da Silva em 11/11 às 13:30 (30 min) com Dra. Ana"));
        assertEquals("2026-11-11T16:30:00Z", params.getValue().get("startAt"));
        assertEquals("2026-11-11T17:00:00Z", params.getValue().get("endAt"));
        assertEquals(userId, params.getValue().get("hostId"));
        verify(appointments, never()).create(any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void usesTypeDuration() {
        UUID typeId = UUID.randomUUID();
        AppointmentTypeResponse type = mock(AppointmentTypeResponse.class);
        when(type.active()).thenReturn(true);
        when(type.durationMinutes()).thenReturn(60);
        when(type.name()).thenReturn("Avaliação");
        when(types.getById(companyId, typeId)).thenReturn(type);

        tool.execute(ctx, Map.of("contactId", contactId.toString(), "startAt", "2026-11-11T13:30",
                "appointmentTypeId", typeId.toString()));

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(actionService).propose(any(), any(), any(), anyString(), anyString(), any(), params.capture(),
                anyString());
        assertEquals("2026-11-11T17:30:00Z", params.getValue().get("endAt"));
        assertEquals(typeId, params.getValue().get("appointmentTypeId"));
    }

    @Test
    void rejectsContactFromAnotherCompanyOrMissing() {
        UUID other = UUID.randomUUID();
        when(contacts.getById(companyId, other)).thenThrow(new RuntimeException("not found"));

        assertThrows(IllegalArgumentException.class, () -> tool.execute(ctx,
                Map.of("contactId", other.toString(), "startAt", "2026-11-11T13:30")));
        assertThrows(IllegalArgumentException.class, () -> tool.execute(ctx,
                Map.of("startAt", "2026-11-11T13:30")));
        verifyNoProposal();
    }

    @Test
    void rejectsPastTime() {
        assertThrows(IllegalArgumentException.class, () -> tool.execute(ctx,
                Map.of("contactId", contactId.toString(), "startAt", "2026-11-04T10:00")));
        verifyNoProposal();
    }

    @Test
    void multipleProfessionalsWithoutHostIdAsksToChoose() {
        when(professionals.list(companyId)).thenReturn(List.of(
                new Professional(UUID.randomUUID(), "Dr. Bruno"), new Professional(UUID.randomUUID(), "Dra. Carla")));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> tool.execute(ctx,
                Map.of("contactId", contactId.toString(), "startAt", "2026-11-11T13:30")));
        assertTrue(e.getMessage().contains("Dr. Bruno"));
        assertTrue(e.getMessage().contains("hostId"));
        verifyNoProposal();
    }

    @Test
    void unknownHostIdRejected() {
        assertThrows(IllegalArgumentException.class, () -> tool.execute(ctx, Map.of("contactId",
                contactId.toString(), "startAt", "2026-11-11T13:30", "hostId", UUID.randomUUID().toString())));
        verifyNoProposal();
    }

    @Test
    void conflictIsWarnedInDescription() {
        AppointmentResponse busy = mock(AppointmentResponse.class);
        when(busy.status()).thenReturn(AppointmentStatus.SCHEDULED);
        when(busy.hostId()).thenReturn(userId);
        when(busy.startAt()).thenReturn(Instant.parse("2026-11-11T16:00:00Z"));
        when(busy.endAt()).thenReturn(Instant.parse("2026-11-11T16:45:00Z"));
        when(appointments.list(eq(companyId), any(), any(), any())).thenReturn(List.of(busy));

        tool.execute(ctx, Map.of("contactId", contactId.toString(), "startAt", "2026-11-11T13:30"));

        ArgumentCaptor<String> description = ArgumentCaptor.forClass(String.class);
        verify(actionService).propose(any(), any(), any(), anyString(), anyString(), any(), any(),
                description.capture());
        assertTrue(description.getValue().contains("não está livre"));
    }

    @Test
    void canceledAppointmentIsNotAConflict() {
        AppointmentResponse canceled = mock(AppointmentResponse.class);
        when(canceled.status()).thenReturn(AppointmentStatus.CANCELED);
        when(canceled.hostId()).thenReturn(userId);
        when(canceled.startAt()).thenReturn(Instant.parse("2026-11-11T16:00:00Z"));
        when(canceled.endAt()).thenReturn(Instant.parse("2026-11-11T16:45:00Z"));
        when(appointments.list(eq(companyId), any(), any(), any())).thenReturn(List.of(canceled));

        tool.execute(ctx, Map.of("contactId", contactId.toString(), "startAt", "2026-11-11T13:30"));

        ArgumentCaptor<String> description = ArgumentCaptor.forClass(String.class);
        verify(actionService).propose(any(), any(), any(), anyString(), anyString(), any(), any(),
                description.capture());
        assertFalse(description.getValue().contains("não está livre"));
    }

    @Test
    void executorCreatesAppointmentWithoutForce() {
        AiActionExecutor executor = new AiActionExecutor(mock(TaskUseCase.class), mock(ActivityUseCase.class),
                mock(OpportunityUseCase.class), appointments, mock(FinancialTransactionService.class),
                mock(InventoryService.class));
        UUID hostId = UUID.randomUUID();
        AiAction action = mock(AiAction.class);
        when(action.getTool()).thenReturn("create_appointment");
        when(action.getCompanyId()).thenReturn(companyId);
        when(action.getUserId()).thenReturn(userId);
        when(action.getParameters()).thenReturn(Map.of("contactId", contactId.toString(),
                "hostId", hostId.toString(), "title", "Consulta - Lúcia",
                "startAt", "2026-11-11T16:30:00Z", "endAt", "2026-11-11T17:00:00Z", "notes", "Facetas"));

        executor.execute(action);

        verify(appointments).create(companyId, new CreateAppointmentRequest(null, hostId, contactId, null,
                "Consulta - Lúcia", Instant.parse("2026-11-11T16:30:00Z"), Instant.parse("2026-11-11T17:00:00Z"),
                null, null, "Facetas", false, null, null, null, null, null), userId);
    }

    private void verifyNoProposal() {
        verify(actionService, never()).propose(any(), any(), any(), anyString(), anyString(), any(), any(),
                anyString());
    }
}
