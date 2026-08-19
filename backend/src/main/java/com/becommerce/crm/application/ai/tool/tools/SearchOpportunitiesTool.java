package com.becommerce.crm.application.ai.tool.tools;

import com.becommerce.crm.application.ai.tool.AbstractAiReadTool;
import com.becommerce.crm.application.ai.tool.AiToolContext;
import com.becommerce.crm.application.ai.tool.AiToolResult;
import com.becommerce.crm.application.pipeline.port.input.OpportunityUseCase;
import com.becommerce.crm.domain.pipeline.OpportunityStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * {@code search_opportunities} (AI-03): busca oportunidades com filtros
 * opcionais (status, pipeline, estágio, responsável, contato), limitada.
 * Reutiliza {@link OpportunityUseCase#search}. Exige {@code opportunity:read}.
 */
@Component
public class SearchOpportunitiesTool extends AbstractAiReadTool {

    private static final String NAME = "search_opportunities";

    private final OpportunityUseCase opportunityUseCase;

    public SearchOpportunitiesTool(OpportunityUseCase opportunityUseCase) {
        super(NAME, "Busca oportunidades com filtros opcionais (status, pipeline, estágio, "
                        + "responsável, contato) — lista limitada.",
                "opportunity:read",
                Map.of("status", stringProp("Status: OPEN, WON ou LOST", false),
                        "pipelineId", stringProp("ID do pipeline", false),
                        "stageId", stringProp("ID do estágio", false),
                        "contactId", stringProp("ID do contato/cliente", false),
                        "assignedTo", stringProp("ID do responsável", false),
                        "limit", integerProp("Quantidade máxima de resultados (máx. 50)")));
        this.opportunityUseCase = opportunityUseCase;
    }

    @Override
    protected AiToolResult doExecute(AiToolContext ctx, Map<String, Object> arguments) {
        OpportunityStatus status = status(arguments);
        UUID pipelineId = uuid(arguments, "pipelineId");
        UUID stageId = uuid(arguments, "stageId");
        UUID contactId = uuid(arguments, "contactId");
        UUID assignedTo = uuid(arguments, "assignedTo");
        int limit = clampLimit(integer(arguments, "limit"));

        var opportunities = opportunityUseCase.search(ctx.companyId(), status, pipelineId,
                contactId, stageId, assignedTo, limit);
        return AiToolResult.ok(NAME, opportunities);
    }

    private OpportunityStatus status(Map<String, Object> arguments) {
        String s = string(arguments, "status");
        if (s == null) {
            return null;
        }
        try {
            return OpportunityStatus.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Status inválido: " + s + " (esperado OPEN, WON ou LOST)");
        }
    }
}