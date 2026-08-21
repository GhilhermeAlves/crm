package com.becommerce.crm.application.ai.tool.tools;

import com.becommerce.crm.application.activity.port.input.ActivityUseCase;
import com.becommerce.crm.application.ai.tool.AbstractAiReadTool;
import com.becommerce.crm.application.ai.tool.AiToolContext;
import com.becommerce.crm.application.ai.tool.AiToolResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code get_activity} (AI-03): obtém os dados de uma atividade por id.
 * Reutiliza {@link ActivityUseCase#getById}. Exige {@code activity:read}.
 */
@Component
public class ActivityTool extends AbstractAiReadTool {

    private static final String NAME = "get_activity";

    private final ActivityUseCase activityUseCase;

    public ActivityTool(ActivityUseCase activityUseCase) {
        super(NAME, "Obtém os dados de uma atividade específica.",
                "activity:read",
                Map.of("activityId", stringProp("ID da atividade")), List.of("activityId"));
        this.activityUseCase = activityUseCase;
    }

    @Override
    protected AiToolResult doExecute(AiToolContext ctx, Map<String, Object> arguments) {
        UUID activityId = uuid(arguments, "activityId");
        if (activityId == null) {
            return AiToolResult.fail(NAME, "Parâmetro obrigatório ausente: activityId");
        }
        var activity = activityUseCase.getById(ctx.companyId(), activityId);
        return AiToolResult.ok(NAME, activity);
    }
}