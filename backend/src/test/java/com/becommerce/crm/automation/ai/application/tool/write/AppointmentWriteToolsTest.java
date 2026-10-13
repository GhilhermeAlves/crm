package com.becommerce.crm.automation.ai.application.tool.write;

import com.becommerce.crm.automation.ai.application.action.AiActionExecutor;
import com.becommerce.crm.automation.ai.application.action.AiActionService;
import com.becommerce.crm.automation.ai.application.context.AiPermissionContext;
import com.becommerce.crm.automation.ai.application.tool.AiToolContext;
import com.becommerce.crm.automation.ai.domain.AiAction;
import com.becommerce.crm.finance.transaction.application.service.FinancialTransactionService;
import com.becommerce.crm.masterdata.inventory.application.service.InventoryService;
import com.becommerce.crm.sales.activity.application.port.in.ActivityUseCase;
import com.becommerce.crm.sales.pipeline.application.port.in.OpportunityUseCase;
import com.becommerce.crm.sales.scheduling.application.dto.AppointmentResponse;
import com.becommerce.crm.sales.scheduling.application.dto.RescheduleRequest;
import com.becommerce.crm.sales.scheduling.application.port.in.AppointmentUseCase;
import com.becommerce.crm.sales.scheduling.domain.AppointmentSource;
import com.becommerce.crm.sales.scheduling.domain.AppointmentStatus;
import com.becommerce.crm.sales.task.application.port.in.TaskUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppointmentWriteToolsTest {

    private final UUID companyId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();
    private final AiToolContext ctx = new AiToolContext(companyId, userId,
            new AiPermissionContext(List.of("appointment:update")), conversationId);

    private final AiActionService actionService = mock(AiActionService.class);
    private final AppointmentUseCase appointments = mock(AppointmentUseCase.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-12T12:00:00Z"), ZoneOffset.UTC);

    private final CancelAppointmentTool cancelTool = new CancelAppointmentTool(actionService, appointments);
    private final RescheduleAppointmentTool rescheduleTool =
            new RescheduleAppointmentTool(actionService, appointments, clock);

    private UUID existing(AppointmentStatus status) {
        UUID id = UUID.randomUUID();
        // 13/10 10:00-10:45 em São Paulo
        Instant start = Instant.parse("2026-10-13T13:00:00Z");
        when(appointments.getById(companyId, id)).thenReturn(new AppointmentResponse(id, companyId, null,
                UUID.randomUUID(), null, null, "Avaliação", start, start.plusSeconds(2700), status,
                AppointmentSource.INTERNAL, null, null, null, null, null, null, null, null, null));
        return id;
    }

    @Test
    @SuppressWarnings("unchecked")
    void cancelCreatesProposalWithoutCancelling() {
        UUID id = existing(AppointmentStatus.SCHEDULED);

        cancelTool.execute(ctx, Map.of("appointmentId", id.toString(), "reason", " Paciente viajou "));

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(actionService).propose(eq(companyId), eq(userId), eq(conversationId), eq("cancel_appointment"),
                eq("APPOINTMENT"), eq(id), params.capture(),
                eq("Desmarcar consulta: Avaliação em 13/10 às 10:00 — motivo: Paciente viajou"));
        assertEquals("Paciente viajou", params.getValue().get("reason"));
        verify(appointments, never()).cancel(any(), any(), any());
    }

    @Test
    void cancelRejectsAlreadyCanceled() {
        UUID id = existing(AppointmentStatus.CANCELED);

        assertThrows(IllegalArgumentException.class,
                () -> cancelTool.execute(ctx, Map.of("appointmentId", id.toString())));
        verify(actionService, never()).propose(any(), any(), any(), anyString(), anyString(), any(), any(), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void rescheduleKeepsDurationAndConvertsFromSaoPaulo() {
        UUID id = existing(AppointmentStatus.CONFIRMED);

        rescheduleTool.execute(ctx, Map.of("appointmentId", id.toString(), "newStart", "2026-10-15T15:00"));

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(actionService).propose(eq(companyId), eq(userId), eq(conversationId),
                eq("reschedule_appointment"), eq("APPOINTMENT"), eq(id), params.capture(),
                eq("Remarcar consulta: Avaliação de 13/10 às 10:00 para 15/10 às 15:00"));
        assertEquals("2026-10-15T18:00:00Z", params.getValue().get("startAt"));
        assertEquals("2026-10-15T18:45:00Z", params.getValue().get("endAt"));
        verify(appointments, never()).reschedule(any(), any(), any());
    }

    @Test
    void rescheduleRejectsPastTime() {
        UUID id = existing(AppointmentStatus.SCHEDULED);

        assertThrows(IllegalArgumentException.class, () -> rescheduleTool.execute(ctx,
                Map.of("appointmentId", id.toString(), "newStart", "2026-10-01T10:00")));
    }

    @Test
    void rescheduleRejectsInvalidFormat() {
        UUID id = existing(AppointmentStatus.SCHEDULED);

        assertThrows(IllegalArgumentException.class, () -> rescheduleTool.execute(ctx,
                Map.of("appointmentId", id.toString(), "newStart", "15/10 15h")));
    }

    @Test
    void executorRunsPersistedParameters() {
        AiActionExecutor executor = new AiActionExecutor(mock(TaskUseCase.class), mock(ActivityUseCase.class),
                mock(OpportunityUseCase.class), appointments, mock(FinancialTransactionService.class),
                mock(InventoryService.class));
        UUID id = UUID.randomUUID();

        AiAction cancel = mock(AiAction.class);
        when(cancel.getTool()).thenReturn("cancel_appointment");
        when(cancel.getCompanyId()).thenReturn(companyId);
        when(cancel.getParameters()).thenReturn(Map.of("appointmentId", id.toString(), "reason", "Viagem"));
        executor.execute(cancel);
        verify(appointments).cancel(companyId, id, "Viagem");

        AiAction reschedule = mock(AiAction.class);
        when(reschedule.getTool()).thenReturn("reschedule_appointment");
        when(reschedule.getCompanyId()).thenReturn(companyId);
        when(reschedule.getParameters()).thenReturn(Map.of("appointmentId", id.toString(),
                "startAt", "2026-10-15T18:00:00Z", "endAt", "2026-10-15T18:45:00Z"));
        executor.execute(reschedule);
        verify(appointments).reschedule(companyId, id, new RescheduleRequest(
                Instant.parse("2026-10-15T18:00:00Z"), Instant.parse("2026-10-15T18:45:00Z"), false));
    }
}
