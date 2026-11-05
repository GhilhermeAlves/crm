package com.becommerce.crm.automation.ai.application.tool.tools;

import com.becommerce.crm.automation.ai.application.tool.AbstractAiReadTool;
import com.becommerce.crm.automation.ai.application.tool.AiToolContext;
import com.becommerce.crm.automation.ai.application.tool.AiToolResult;
import com.becommerce.crm.sales.scheduling.application.dto.AppointmentTypeResponse;
import com.becommerce.crm.sales.scheduling.application.port.in.AppointmentTypeUseCase;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code search_appointment_types}: tipos de consulta ativos (id, nome, duracao,
 * responsaveis) para {@code create_appointment}. Exige {@code appointment:read}.
 */
@Component
public class SearchAppointmentTypesTool extends AbstractAiReadTool {

    private static final String NAME = "search_appointment_types";

    private final AppointmentTypeUseCase appointmentTypeUseCase;

    public SearchAppointmentTypesTool(AppointmentTypeUseCase appointmentTypeUseCase) {
        super(NAME, "Lista os tipos de consulta ativos (id, nome, duracao em minutos e responsaveis).",
                "appointment:read", Map.of());
        this.appointmentTypeUseCase = appointmentTypeUseCase;
    }

    @Override
    protected AiToolResult doExecute(AiToolContext ctx, Map<String, Object> arguments) {
        var types = appointmentTypeUseCase.listByCompany(ctx.companyId()).stream()
                .filter(AppointmentTypeResponse::active)
                .map(SearchAppointmentTypesTool::toMap)
                .toList();
        return AiToolResult.ok(NAME, types);
    }

    private static Map<String, Object> toMap(AppointmentTypeResponse t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.id());
        m.put("name", t.name());
        m.put("durationMinutes", t.durationMinutes());
        m.put("hostIds", t.hostIds());
        return m;
    }
}
