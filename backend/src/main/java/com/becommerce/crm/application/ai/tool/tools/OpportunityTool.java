package com.becommerce.crm.application.ai.tool.tools;

import com.becommerce.crm.application.ai.tool.AbstractAiReadTool;
import com.becommerce.crm.application.ai.tool.AiToolContext;
import com.becommerce.crm.application.ai.tool.AiToolResult;
import com.becommerce.crm.application.pipeline.port.input.OpportunityUseCase;
import com.becommerce.crm.domain.pipeline.OpportunityStatus;
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
