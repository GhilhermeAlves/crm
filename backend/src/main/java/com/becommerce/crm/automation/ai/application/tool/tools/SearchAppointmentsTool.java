package com.becommerce.crm.automation.ai.application.tool.tools;

import com.becommerce.crm.automation.ai.application.tool.AbstractAiReadTool;
import com.becommerce.crm.automation.ai.application.tool.AgendaFormat;
import com.becommerce.crm.automation.ai.application.tool.AiToolContext;
import com.becommerce.crm.automation.ai.application.tool.AiToolResult;
import com.becommerce.crm.masterdata.contact.application.dto.response.ContactResponse;
import com.becommerce.crm.masterdata.contact.application.port.in.ContactUseCase;
import com.becommerce.crm.sales.scheduling.application.dto.AppointmentResponse;
import com.becommerce.crm.sales.scheduling.application.port.in.AppointmentUseCase;
import com.becommerce.crm.sales.scheduling.domain.AppointmentStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * {@code search_appointments}: lista consultas da agenda em uma janela de dias
 * (horário de São Paulo), opcionalmente filtrando por paciente. Fornece o
 * {@code appointmentId} usado por {@code cancel_appointment} e
 * {@code reschedule_appointment}. Exige {@code appointment:read}.
 */
@Component
public class SearchAppointmentsTool extends AbstractAiReadTool {

    private static final String NAME = "search_appointments";
    private static final int MAX_DAYS = 14;
    private static final int MAX_RESULTS = 50;

    private final AppointmentUseCase appointmentUseCase;
    private final ContactUseCase contactUseCase;
    private final Clock clock;

    @Autowired
    public SearchAppointmentsTool(AppointmentUseCase appointmentUseCase, ContactUseCase contactUseCase) {
        this(appointmentUseCase, contactUseCase, Clock.system(AgendaFormat.CLINIC_ZONE));
    }

    SearchAppointmentsTool(AppointmentUseCase appointmentUseCase, ContactUseCase contactUseCase, Clock clock) {
        super(NAME, "Busca consultas da agenda a partir de uma data (padrão: hoje) por N dias, "
                        + "opcionalmente de um paciente. Horários no fuso de Brasília. Use antes de "
                        + "desmarcar ou remarcar para obter o appointmentId.",
                "appointment:read",
                Map.of("date", stringProp("Data inicial no formato AAAA-MM-DD (padrão: hoje)"),
                        "days", integerProp("Quantidade de dias a partir da data (1 a 14, padrão 1)"),
                        "patient", stringProp("Nome ou telefone do paciente (opcional)"),
                        "includeCanceled", Map.of("type", "boolean",
                                "description", "Incluir consultas canceladas (padrão: false)")));
        this.appointmentUseCase = appointmentUseCase;
        this.contactUseCase = contactUseCase;
        this.clock = clock;
    }

    public record AppointmentSummary(UUID id, String title, String start, String end, String status,
                                     String patientName) {
    }

    @Override
    protected AiToolResult doExecute(AiToolContext ctx, Map<String, Object> arguments) {
        LocalDate date = parseDate(string(arguments, "date"));
        Integer rawDays = integer(arguments, "days");
        int days = rawDays == null || rawDays < 1 ? 1 : Math.min(rawDays, MAX_DAYS);
        boolean includeCanceled = Boolean.parseBoolean(string(arguments, "includeCanceled"));

        Set<UUID> patientIds = null;
        String patient = string(arguments, "patient");
        if (patient != null) {
            patientIds = contactUseCase.search(ctx.companyId(), patient, 5).stream()
                    .map(ContactResponse::id).collect(Collectors.toSet());
            if (patientIds.isEmpty()) {
                return AiToolResult.fail(NAME, "Nenhum paciente encontrado para '" + patient + "'.");
            }
        }

        Instant from = date.atStartOfDay(AgendaFormat.CLINIC_ZONE).toInstant();
        Instant to = date.plusDays(days).atStartOfDay(AgendaFormat.CLINIC_ZONE).toInstant();
        Set<UUID> filter = patientIds;
        List<AppointmentResponse> appointments = appointmentUseCase.list(ctx.companyId(), from, to, null)
                .stream()
                .filter(a -> includeCanceled || a.status() != AppointmentStatus.CANCELED)
                .filter(a -> filter == null || (a.contactId() != null && filter.contains(a.contactId())))
                .sorted(Comparator.comparing(AppointmentResponse::startAt))
                .limit(MAX_RESULTS)
                .toList();

        Map<UUID, String> names = new HashMap<>();
        List<AppointmentSummary> summaries = appointments.stream()
                .map(a -> new AppointmentSummary(a.id(), a.title(), AgendaFormat.dateTime(a.startAt()),
                        AgendaFormat.dateTime(a.endAt()), a.status().name(),
                        patientName(ctx.companyId(), a.contactId(), names)))
                .toList();
        return AiToolResult.ok(NAME, summaries);
    }

    private LocalDate parseDate(String raw) {
        if (raw == null) {
            return LocalDate.now(clock.withZone(AgendaFormat.CLINIC_ZONE));
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data inválida: use o formato AAAA-MM-DD.");
        }
    }

    private String patientName(UUID companyId, UUID contactId, Map<UUID, String> cache) {
        if (contactId == null) {
            return null;
        }
        return cache.computeIfAbsent(contactId, id -> {
            try {
                ContactResponse c = contactUseCase.getById(companyId, id);
                String full = ((c.firstName() == null ? "" : c.firstName()) + " "
                        + (c.lastName() == null ? "" : c.lastName())).trim();
                return full.isEmpty() ? null : full;
            } catch (RuntimeException e) {
                return null;
            }
        });
    }
}
