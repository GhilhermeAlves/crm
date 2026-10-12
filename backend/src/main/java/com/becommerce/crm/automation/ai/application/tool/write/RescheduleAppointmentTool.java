package com.becommerce.crm.automation.ai.application.tool.write;

import com.becommerce.crm.automation.ai.application.action.AiActionService;
import com.becommerce.crm.automation.ai.application.dto.AiActionResponse;
import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import com.becommerce.crm.automation.ai.application.tool.AgendaFormat;
import com.becommerce.crm.automation.ai.application.tool.AiTool;
import com.becommerce.crm.automation.ai.application.tool.AiToolContext;
import com.becommerce.crm.automation.ai.application.tool.AiToolResult;
import com.becommerce.crm.sales.scheduling.application.dto.AppointmentResponse;
import com.becommerce.crm.sales.scheduling.application.port.in.AppointmentUseCase;
import com.becommerce.crm.sales.scheduling.domain.AppointmentStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.newMap;
import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.put;
import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.text;
import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.uuid;

/**
 * Write tool {@code reschedule_appointment}: recebe o novo inicio em horario
 * local de Sao Paulo, preserva a duracao original e PERSISTE uma proposta com
 * {@code startAt}/{@code endAt} em ISO-8601. Conflitos de horario sao checados
 * pelo dominio na confirmacao. Requer {@code appointment:update}.
 */
@Component
public class RescheduleAppointmentTool implements AiTool {

    static final String NAME = "reschedule_appointment";

    private final AiActionService actionService;
    private final AppointmentUseCase appointmentUseCase;
    private final Clock clock;

    @Autowired
    public RescheduleAppointmentTool(@Lazy AiActionService actionService, AppointmentUseCase appointmentUseCase) {
        this(actionService, appointmentUseCase, Clock.systemUTC());
    }

    RescheduleAppointmentTool(AiActionService actionService, AppointmentUseCase appointmentUseCase, Clock clock) {
        this.actionService = actionService;
        this.appointmentUseCase = appointmentUseCase;
        this.clock = clock;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Propoe REMARCAR uma consulta para um novo horario (mantem a duracao). Nao executa: cria uma "
                + "proposta que o usuario confirma. Antes, use search_appointments para obter o appointmentId.";
    }

    @Override
    public String requiredPermission() {
        return "appointment:update";
    }

    @Override
    public String risk() {
        return "WRITE";
    }

    @Override
    public Map<String, Object> inputSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                "appointmentId", Map.of("type", "string", "format", "uuid", "description",
                        "id EXATO retornado por search_appointments; nunca invente nem use texto de exemplo"),
                "newStart", Map.of("type", "string", "description",
                        "Novo inicio em horario de Brasilia, formato AAAA-MM-DDTHH:mm")));
        schema.put("required", List.of("appointmentId", "newStart"));
        return schema;
    }

    @Override
    public AiProvider.ToolDefinition toolDefinition() {
        return new AiProvider.ToolDefinition(name(), description(), inputSchema());
    }

    @Override
    public AiToolResult execute(AiToolContext ctx, Map<String, Object> args) {
        UUID appointmentId = uuid(args, "appointmentId");
        if (appointmentId == null) {
            throw new IllegalArgumentException("Informe o appointmentId da consulta.");
        }
        Instant newStart = parseLocal(text(args, "newStart"));
        if (!newStart.isAfter(clock.instant())) {
            throw new IllegalArgumentException("O novo horario precisa estar no futuro.");
        }
        AppointmentResponse appointment = appointmentUseCase.getById(ctx.companyId(), appointmentId);
        if (appointment.status() == AppointmentStatus.CANCELED
                || appointment.status() == AppointmentStatus.COMPLETED) {
            throw new IllegalArgumentException("Esta consulta nao pode ser remarcada (status: "
                    + appointment.status() + ").");
        }
        Instant newEnd = newStart.plus(Duration.between(appointment.startAt(), appointment.endAt()));

        Map<String, Object> parameters = newMap();
        put(parameters, "appointmentId", appointmentId);
        put(parameters, "startAt", newStart.toString());
        put(parameters, "endAt", newEnd.toString());

        String description = "Remarcar consulta: " + appointment.title() + " de "
                + AgendaFormat.shortDateTime(appointment.startAt()) + " para "
                + AgendaFormat.shortDateTime(newStart);
        AiActionResponse proposal = actionService.propose(ctx.companyId(), ctx.userId(), ctx.conversationId(),
                NAME, "APPOINTMENT", appointmentId, parameters, description);
        return AiToolResult.ok(NAME, proposal);
    }

    private static Instant parseLocal(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Informe o novo horario (newStart).");
        }
        try {
            return LocalDateTime.parse(raw.trim()).atZone(AgendaFormat.CLINIC_ZONE).toInstant();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Horario invalido: use AAAA-MM-DDTHH:mm.");
        }
    }
}
