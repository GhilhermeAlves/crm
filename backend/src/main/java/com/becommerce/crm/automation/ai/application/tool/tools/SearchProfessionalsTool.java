package com.becommerce.crm.automation.ai.application.tool.tools;

import com.becommerce.crm.automation.ai.application.tool.AbstractAiReadTool;
import com.becommerce.crm.automation.ai.application.tool.AiToolContext;
import com.becommerce.crm.automation.ai.application.tool.AiToolResult;
import com.becommerce.crm.automation.ai.application.tool.SchedulingProfessionals;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

/**
 * {@code search_professionals}: profissionais da clinica que podem ser
 * responsaveis por agenda (hostId de {@code create_appointment}). Exige
 * {@code appointment:read}.
 */
@Component
public class SearchProfessionalsTool extends AbstractAiReadTool {

    private static final String NAME = "search_professionals";

    private final SchedulingProfessionals professionals;

    public SearchProfessionalsTool(SchedulingProfessionals professionals) {
        super(NAME, "Lista os profissionais da clinica que podem ser responsaveis por consultas "
                        + "(id para o hostId de create_appointment). Filtro opcional por nome.",
                "appointment:read",
                Map.of("query", stringProp("Parte do nome do profissional (opcional)")));
        this.professionals = professionals;
    }

    @Override
    protected AiToolResult doExecute(AiToolContext ctx, Map<String, Object> arguments) {
        String query = normalize(string(arguments, "query"));
        var result = professionals.list(ctx.companyId()).stream()
                .filter(p -> query == null || normalize(p.name()).contains(query))
                .toList();
        return AiToolResult.ok(NAME, result);
    }

    private static String normalize(String s) {
        if (s == null) {
            return null;
        }
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
