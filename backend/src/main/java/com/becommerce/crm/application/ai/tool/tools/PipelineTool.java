package com.becommerce.crm.application.ai.tool.tools;

import com.becommerce.crm.application.ai.tool.AbstractAiReadTool;
import com.becommerce.crm.application.ai.tool.AiToolContext;
import com.becommerce.crm.application.ai.tool.AiToolResult;
import com.becommerce.crm.application.pipeline.port.input.PipelineUseCase;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * {@code get_pipeline} (AI-03): obtém os dados de um pipeline (com estágios)
 * por id. Reutiliza {@link PipelineUseCase#getById}. Exige {@code pipeline:view}.
 */
@Component
public class PipelineTool extends AbstractAiReadTool {

    private static final String NAME = "get_pipeline";

    private final PipelineUseCase pipelineUseCase;

    public PipelineTool(PipelineUseCase pipelineUseCase) {
        super(NAME, "Obtém os dados de um pipeline (incluindo estágios).",
                "pipeline:view",
                Map.of("pipelineId", stringProp("ID do pipeline", true)));
        this.pipelineUseCase = pipelineUseCase;
    }

    @Override
    protected AiToolResult doExecute(AiToolContext ctx, Map<String, Object> arguments) {
        UUID pipelineId = uuid(arguments, "pipelineId");
        if (pipelineId == null) {
            return AiToolResult.fail(NAME, "Parâmetro obrigatório ausente: pipelineId");
        }
        var pipeline = pipelineUseCase.getById(ctx.companyId(), pipelineId);
        return AiToolResult.ok(NAME, pipeline);
    }
}