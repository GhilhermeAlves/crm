package com.becommerce.crm.automation.ai.application.agent.tool;

import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import com.becommerce.crm.sales.followup.application.dto.FollowUpRequest;
import com.becommerce.crm.sales.followup.application.dto.FollowUpResponse;
import com.becommerce.crm.sales.followup.application.port.in.FollowUpUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RescheduleReminderToolProviderTest {

    private final UUID companyId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();
    private final FollowUpUseCase followUps = mock(FollowUpUseCase.class);

    private AgentToolSession session() {
        return new AgentToolSession(companyId, UUID.randomUUID(), conversationId, null, "5534999990000",
                "Maria", UUID.randomUUID());
    }

    /** Relógio do servidor em UTC (como em produção), no instante dado em horário de Brasília. */
    private FollowUpRequest scheduleAt(ZonedDateTime nowInBrasilia) {
        when(followUps.create(any(), any())).thenReturn(mock(FollowUpResponse.class));
        Clock utc = Clock.fixed(nowInBrasilia.toInstant(), ZoneOffset.UTC);
        RescheduleReminderToolProvider tool = new RescheduleReminderToolProvider(followUps, utc);
        String result = tool.execute(session(), new AiProvider.ToolCall("1",
                RescheduleReminderToolProvider.REMIND, Map.of("mensagem", "Oi, Maria! Vamos remarcar?")));
        assertTrue(result.startsWith("Lembrete agendado"), result);
        ArgumentCaptor<FollowUpRequest> captor = ArgumentCaptor.forClass(FollowUpRequest.class);
        verify(followUps).create(eq(companyId), captor.capture());
        return captor.getValue();
    }

    private static ZonedDateTime brasilia(int month, int day, int hour) {
        return ZonedDateTime.of(2026, month, day, hour, 0, 0, 0, RescheduleReminderToolProvider.CLINIC_ZONE);
    }

    @Test
    void twoDaysLater_at10hBrasilia_storedInServerUtc() {
        // terça 13/10 → quinta 15/10, 10h de Brasília = 13h UTC
        FollowUpRequest req = scheduleAt(brasilia(10, 13, 15));
        assertEquals(LocalDateTime.of(2026, 10, 15, 13, 0), req.executeAt());
        assertEquals(conversationId, req.conversationId());
        assertEquals("Oi, Maria! Vamos remarcar?", req.content());
    }

    @Test
    void landingOnWeekend_skipsToNextBusinessDay_overHoliday() {
        // quinta 08/10 + 2 = sábado 10/10 → segunda 12/10 é feriado → terça 13/10
        assertEquals(LocalDateTime.of(2026, 10, 13, 13, 0), scheduleAt(brasilia(10, 8, 9)).executeAt());
    }

    @Test
    void lateNightInBrasilia_usesBrasiliaDate() {
        // terça 13/10 às 23h em Brasília já é quarta 14/10 em UTC; conta a partir de terça → quinta 15/10
        assertEquals(LocalDateTime.of(2026, 10, 15, 13, 0), scheduleAt(brasilia(10, 13, 23)).executeAt());
    }

    @Test
    void nextBusinessDay_skipsWeekendsAndHolidays() {
        // sábado 31/10 → domingo → segunda 02/11 (Finados) → terça 03/11
        assertEquals(LocalDate.of(2026, 11, 3),
                RescheduleReminderToolProvider.nextBusinessDay(LocalDate.of(2026, 10, 31)));
        assertEquals(LocalDate.of(2026, 10, 14),
                RescheduleReminderToolProvider.nextBusinessDay(LocalDate.of(2026, 10, 14)));
    }

    @Test
    void sameConversationAndDay_usesSameIdempotencyKey() {
        UUID first = scheduleAt(brasilia(10, 13, 15)).idempotencyKey();
        reset(followUps);
        UUID second = scheduleAt(brasilia(10, 13, 17)).idempotencyKey();
        assertEquals(first, second);
    }

    @Test
    void failure_tellsAgentNotToPromiseAReturn() {
        when(followUps.create(any(), any())).thenThrow(new RuntimeException("conversa em modo humano"));
        String result = new RescheduleReminderToolProvider(followUps).execute(session(),
                new AiProvider.ToolCall("1", RescheduleReminderToolProvider.REMIND, Map.of("mensagem", "Oi!")));
        assertTrue(result.contains("Não prometa retorno"), result);
    }

    @Test
    void blankMessage_isRejected() {
        String result = new RescheduleReminderToolProvider(followUps).execute(session(),
                new AiProvider.ToolCall("1", RescheduleReminderToolProvider.REMIND, Map.of("mensagem", " ")));
        assertTrue(result.startsWith("Lembrete não agendado"), result);
        verifyNoInteractions(followUps);
    }
}
