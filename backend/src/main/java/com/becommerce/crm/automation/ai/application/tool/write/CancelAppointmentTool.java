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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.newMap;
import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.put;
import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.text;
import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.uuid;

/**
 * Write tool {@code cancel_appointment}: valida que a consulta e da empresa
 * ativa e ainda pode ser desmarcada, e PERSISTE uma proposta. O cancelamento so
 * ocorre apos confirmacao explicita do usuario. Requer {@code appointment:update}.
 */
@Component
public class CancelAppointmentTool implements AiTool {

    static final String NAME = "cancel_appointment";
    private static final int MAX_REASON = 500;

    private final AiActionService actionService;
    private final AppointmentUseCase appointmentUseCase;

    public CancelAppointmentTool(@Lazy AiActionService actionService, AppointmentUseCase appointmentUseCase) {
        this.actionService = actionService;
        this.appointmentUseCase = appointmentUseCase;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Propoe DESMARCAR (cancelar) uma consulta. Nao executa: cria uma proposta que o usuario "
                + "confirma. Antes, use search_appointments para obter o appointmentId.";
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
                "appointmentId", Map.of("type", "string", "format", "uuid"),
                "reason", Map.of("type", "string", "description", "Motivo do cancelamento (max 500)")));
        schema.put("required", List.of("appointmentId"));
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
        AppointmentResponse appointment = appointmentUseCase.getById(ctx.companyId(), appointmentId);
        if (appointment.status() == AppointmentStatus.CANCELED
                || appointment.status() == AppointmentStatus.COMPLETED) {
            throw new IllegalArgumentException("Esta consulta nao pode ser desmarcada (status: "
                    + appointment.status() + ").");
        }
        String reason = text(args, "reason");
        if (reason != null) {
            reason = reason.trim();
            if (reason.length() > MAX_REASON) {
                reason = reason.substring(0, MAX_REASON);
            }
        }

        Map<String, Object> parameters = newMap();
        put(parameters, "appointmentId", appointmentId);
        put(parameters, "reason", reason);

        String description = "Desmarcar consulta: " + appointment.title() + " em "
                + AgendaFormat.shortDateTime(appointment.startAt())
                + (reason != null ? " — motivo: " + reason : "");
        AiActionResponse proposal = actionService.propose(ctx.companyId(), ctx.userId(), ctx.conversationId(),
                NAME, "APPOINTMENT", appointmentId, parameters, description);
        return AiToolResult.ok(NAME, proposal);
    }
}
