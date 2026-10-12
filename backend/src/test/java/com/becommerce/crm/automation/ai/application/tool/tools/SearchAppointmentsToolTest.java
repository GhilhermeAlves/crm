package com.becommerce.crm.automation.ai.application.tool.tools;

import com.becommerce.crm.automation.ai.application.context.AiPermissionContext;
import com.becommerce.crm.automation.ai.application.tool.AiToolContext;
import com.becommerce.crm.automation.ai.application.tool.AiToolResult;
import com.becommerce.crm.automation.ai.application.tool.tools.SearchAppointmentsTool.AppointmentSummary;
import com.becommerce.crm.masterdata.contact.application.dto.response.ContactResponse;
import com.becommerce.crm.masterdata.contact.application.port.in.ContactUseCase;
import com.becommerce.crm.sales.scheduling.application.dto.AppointmentResponse;
import com.becommerce.crm.sales.scheduling.application.port.in.AppointmentUseCase;
import com.becommerce.crm.sales.scheduling.domain.AppointmentSource;
import com.becommerce.crm.sales.scheduling.domain.AppointmentStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchAppointmentsToolTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    // 12/10/2026 09:00 em São Paulo
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-12T12:00:00Z"), SP);
    private final AppointmentUseCase appointments = mock(AppointmentUseCase.class);
    private final ContactUseCase contacts = mock(ContactUseCase.class);
    private final SearchAppointmentsTool tool = new SearchAppointmentsTool(appointments, contacts, clock);
    private final UUID companyId = UUID.randomUUID();
    private final AiToolContext ctx = new AiToolContext(companyId, UUID.randomUUID(),
            new AiPermissionContext(List.of("appointment:read")));

    private AppointmentResponse appt(UUID contactId, String start, AppointmentStatus status) {
        Instant s = Instant.parse(start);
        return new AppointmentResponse(UUID.randomUUID(), companyId, null, UUID.randomUUID(), contactId, null,
                "Consulta", s, s.plusSeconds(3600), status, AppointmentSource.INTERNAL, null, null, null,
                null, null, null, null, null, null);
    }

    @SuppressWarnings("unchecked")
    private List<AppointmentSummary> data(AiToolResult result) {
        assertTrue(result.success(), result.error());
        return (List<AppointmentSummary>) result.data();
    }

    @Test
    void defaultsToTodayInSaoPauloAndHidesCanceled() {
        Instant from = Instant.parse("2026-10-12T03:00:00Z");
        Instant to = Instant.parse("2026-10-13T03:00:00Z");
        when(appointments.list(companyId, from, to, null)).thenReturn(List.of(
                appt(null, "2026-10-12T17:00:00Z", AppointmentStatus.SCHEDULED),
                appt(null, "2026-10-12T13:00:00Z", AppointmentStatus.CONFIRMED),
                appt(null, "2026-10-12T14:00:00Z", AppointmentStatus.CANCELED)));

        List<AppointmentSummary> result = data(tool.execute(ctx, Map.of()));

        assertEquals(2, result.size());
        assertEquals("12/10/2026 10:00", result.get(0).start());
        assertEquals("12/10/2026 14:00", result.get(1).start());
    }

    @Test
    void filtersByPatientAndResolvesName() {
        UUID maria = UUID.randomUUID();
        ContactResponse contact = mock(ContactResponse.class);
        when(contact.id()).thenReturn(maria);
        when(contact.firstName()).thenReturn("Maria");
        when(contact.lastName()).thenReturn("Souza");
        when(contacts.search(companyId, "maria", 5)).thenReturn(List.of(contact));
        when(contacts.getById(companyId, maria)).thenReturn(contact);
        when(appointments.list(eq(companyId), any(), any(), isNull())).thenReturn(List.of(
                appt(maria, "2026-10-13T13:00:00Z", AppointmentStatus.SCHEDULED),
                appt(UUID.randomUUID(), "2026-10-13T14:00:00Z", AppointmentStatus.SCHEDULED)));

        List<AppointmentSummary> result = data(tool.execute(ctx,
                Map.of("date", "2026-10-13", "days", 3, "patient", "maria")));

        assertEquals(1, result.size());
        assertEquals("Maria Souza", result.get(0).patientName());
        verify(appointments).list(companyId, Instant.parse("2026-10-13T03:00:00Z"),
                Instant.parse("2026-10-16T03:00:00Z"), null);
    }

    @Test
    void unknownPatientFails() {
        when(contacts.search(companyId, "ninguem", 5)).thenReturn(List.of());

        AiToolResult result = tool.execute(ctx, Map.of("patient", "ninguem"));

        assertFalse(result.success());
    }

    @Test
    void invalidDateFails() {
        AiToolResult result = tool.execute(ctx, Map.of("date", "13/10/2026"));

        assertFalse(result.success());
        assertEquals("Data inválida: use o formato AAAA-MM-DD.", result.error());
    }
}
