package com.becommerce.crm.application.ai.tool.tools;

import com.becommerce.crm.application.ai.tool.AbstractAiReadTool;
import com.becommerce.crm.application.ai.tool.AiToolContext;
import com.becommerce.crm.application.ai.tool.AiToolResult;
import com.becommerce.crm.application.task.port.input.TaskUseCase;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code get_task} (AI-03): obtém os dados de uma tarefa por id. Reutiliza
 * {@link TaskUseCase#getById}. Exige {@code task:read}.
 */
@Component
public class TaskTool extends AbstractAiReadTool {

    private static final String NAME = "get_task";

    private final TaskUseCase taskUseCase;

    public TaskTool(TaskUseCase taskUseCase) {
        super(NAME, "Obtém os dados de uma tarefa específica.",
                "task:read",
                Map.of("taskId", stringProp("ID da tarefa")), List.of("taskId"));
        this.taskUseCase = taskUseCase;
    }

    @Override
    protected AiToolResult doExecute(AiToolContext ctx, Map<String, Object> arguments) {
        UUID taskId = uuid(arguments, "taskId");
        if (taskId == null) {
            return AiToolResult.fail(NAME, "Parâmetro obrigatório ausente: taskId");
        }
        var task = taskUseCase.getById(ctx.companyId(), taskId);
        return AiToolResult.ok(NAME, task);
    }
}
