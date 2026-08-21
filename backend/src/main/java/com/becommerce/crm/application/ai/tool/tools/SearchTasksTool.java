package com.becommerce.crm.application.ai.tool.tools;

import com.becommerce.crm.application.ai.tool.AbstractAiReadTool;
import com.becommerce.crm.application.ai.tool.AiToolContext;
import com.becommerce.crm.application.ai.tool.AiToolResult;
import com.becommerce.crm.application.task.port.input.TaskUseCase;
import com.becommerce.crm.domain.task.TaskStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * {@code search_tasks} (AI-03): busca tarefas por status (opcional), limitada.
 * Reutiliza {@link TaskUseCase#listByCompany}. Exige {@code task:read}.
 */
@Component
public class SearchTasksTool extends AbstractAiReadTool {

    private static final String NAME = "search_tasks";

    private final TaskUseCase taskUseCase;

    public SearchTasksTool(TaskUseCase taskUseCase) {
        super(NAME, "Busca tarefas por status (opcional) — lista limitada.",
                "task:read",
                Map.of("status", stringProp("Status: PENDING, IN_PROGRESS, COMPLETED ou CANCELLED"),
                        "limit", integerProp("Quantidade máxima de resultados (máx. 50)")));
        this.taskUseCase = taskUseCase;
    }

    @Override
    protected AiToolResult doExecute(AiToolContext ctx, Map<String, Object> arguments) {
        String rawStatus = string(arguments, "status");
        TaskStatus status = null;
        if (rawStatus != null) {
            try {
                status = TaskStatus.valueOf(rawStatus.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Status inválido: " + rawStatus
                        + " (esperado PENDING, IN_PROGRESS, COMPLETED ou CANCELLED)");
            }
        }
        int limit = clampLimit(integer(arguments, "limit"));

        var tasks = taskUseCase.listByCompany(ctx.companyId(), status).stream()
                .limit(limit).toList();
        return AiToolResult.ok(NAME, tasks);
    }
}