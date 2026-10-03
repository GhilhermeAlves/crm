package com.becommerce.crm.automation.ai.application.tool.tools;

import com.becommerce.crm.automation.ai.application.tool.AbstractAiReadTool;
import com.becommerce.crm.automation.ai.application.tool.AiToolContext;
import com.becommerce.crm.automation.ai.application.tool.AiToolResult;
import com.becommerce.crm.sales.pipeline.application.port.in.OpportunityUseCase;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code get_opportunity} (AI-03): obtém os dados de uma oportunidade por id.
 * Reutiliza {@link OpportunityUseCase#getById}. Exige {@code opportunity:read}.
 */
@Component
public class OpportunityTool extends AbstractAiReadTool {

    private static final String NAME = "get_opportunity";

    private final OpportunityUseCase opportunityUseCase;

    public OpportunityTool(OpportunityUseCase opportunityUseCase) {
        super(NAME, "Obtém os dados de uma oportunidade específica.",
                "opportunity:read",
                Map.of("opportunityId", stringProp("ID da oportunidade")), List.of("opportunityId"));
        this.opportunityUseCase = opportunityUseCase;
    }

    @Override
    protected AiToolResult doExecute(AiToolContext ctx, Map<String, Object> arguments) {
        UUID opportunityId = uuid(arguments, "opportunityId");
        if (opportunityId == null) {
            return AiToolResult.fail(NAME, "Parâmetro obrigatório ausente: opportunityId");
        }
        var opportunity = opportunityUseCase.getById(ctx.companyId(), opportunityId);
        return AiToolResult.ok(NAME, opportunity);
    }
}
