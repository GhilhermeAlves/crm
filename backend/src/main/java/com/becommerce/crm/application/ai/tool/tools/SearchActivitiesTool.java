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
 * {@code search_activities} (AI-03): busca atividades por contato, oportunidade
 * ou da empresa toda, limitada. Reutiliza {@link ActivityUseCase}. Exige
 * {@code activity:read}.
 */
@Component
public class SearchActivitiesTool extends AbstractAiReadTool {

    private static final String NAME = "search_activities";

    private final ActivityUseCase activityUseCase;

    public SearchActivitiesTool(ActivityUseCase activityUseCase) {
        super(NAME, "Busca atividades por contato, oportunidade ou da empresa toda "
                        + "(lista limitada).",
                "activity:read",
                Map.of("contactId", stringProp("ID do contato", false),
                        "opportunityId", stringProp("ID da oportunidade", false),
                        "limit", integerProp("Quantidade máxima de resultados (máx. 50)")));
        this.activityUseCase = activityUseCase;
    }

    @Override
    protected AiToolResult doExecute(AiToolContext ctx, Map<String, Object> arguments) {
        UUID contactId = uuid(arguments, "contactId");
        UUID opportunityId = uuid(arguments, "opportunityId");
        int limit = clampLimit(integer(arguments, "limit"));

        List<?> activities;
        if (opportunityId != null) {
            activities = activityUseCase.listByOpportunity(ctx.companyId(), opportunityId).stream()
                    .limit(limit).toList();
        } else if (contactId != null) {
            activities = activityUseCase.listByContact(ctx.companyId(), contactId).stream()
                    .limit(limit).toList();
        } else {
            activities = activityUseCase.listByCompany(ctx.companyId()).stream()
                    .limit(limit).toList();
        }
        return AiToolResult.ok(NAME, activities);
    }
}