package com.becommerce.crm.application.ai.action;

import com.becommerce.crm.application.activity.dto.CreateActivityRequest;
import com.becommerce.crm.application.activity.port.input.ActivityUseCase;
import com.becommerce.crm.application.pipeline.dto.UpdateOpportunityRequest;
import com.becommerce.crm.application.pipeline.port.input.OpportunityUseCase;
import com.becommerce.crm.application.task.dto.CreateTaskRequest;
import com.becommerce.crm.application.task.port.input.TaskUseCase;
import com.becommerce.crm.domain.activity.ActivityType;
import com.becommerce.crm.domain.ai.AiAction;
import com.becommerce.crm.domain.task.TaskPriority;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Executor de acoes de escrita do assistente de IA (AI-05). Dispara a acao
 * confirmada para o service de dominio correspondente (Task/Activity/
 * Opportunity), reutilizando validacoes, tenant, auditoria e eventos ja
 * existentes. Os parametros tipados sao lidos da proposta PERSISTIDA (JSONB) -
 * nunca de entrada direta do LLM. Retorna o resultado estruturado do dominio.
 */
@Component
public class AiActionExecutor {

    private final TaskUseCase taskUseCase;
    private final ActivityUseCase activityUseCase;
    private final OpportunityUseCase opportunityUseCase;

    public AiActionExecutor(TaskUseCase taskUseCase,
                            ActivityUseCase activityUseCase,
                            OpportunityUseCase opportunityUseCase) {
        this.taskUseCase = taskUseCase;
        this.activityUseCase = activityUseCase;
        this.opportunityUseCase = opportunityUseCase;
    }

    public Object execute(AiAction action) {
        Map<String, Object> p = action.getParameters();
        return switch (action.getTool()) {
            case "create_task" -> taskUseCase.create(
                    action.getCompanyId(),
                    new CreateTaskRequest(
                            uuid(p.get("contactId")),
                            uuid(p.get("opportunityId")),
                            str(p.get("title")),
                            str(p.get("description")),
                            uuid(p.get("assigneeId")),
                            localDateTime(p.get("dueAt")),
                            enumOf(p.get("priority"), TaskPriority.class)),
                    action.getUserId());
            case "create_activity" -> activityUseCase.create(
                    action.getCompanyId(),
                    new CreateActivityRequest(
                            uuid(p.get("contactId")),
                            uuid(p.get("opportunityId")),
                            enumOf(p.get("type"), ActivityType.class),
                            str(p.get("subject")),
                            str(p.get("description")),
                            localDateTime(p.get("activityAt"))),
                    action.getUserId());
            case "update_opportunity" -> opportunityUseCase.update(
                    action.getCompanyId(),
                    action.getEntityId(),
                    new UpdateOpportunityRequest(
                            str(p.get("title")),
                            decimal(p.get("value")),
                            uuid(p.get("assignedTo")),
                            localDateTime(p.get("expectedCloseDate")),
                            str(p.get("notes"))),
                    action.getUserId());
            default -> throw new IllegalArgumentException(
                    "Ferramenta de escrita nao suportada: " + action.getTool());
        };
    }

    static UUID uuid(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof UUID u ? u : UUID.fromString(String.valueOf(value));
    }

    static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    static BigDecimal decimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(String.valueOf(value));
    }

    static LocalDateTime localDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt;
        }
        return LocalDateTime.parse(String.valueOf(value));
    }

    static <E extends Enum<E>> E enumOf(Object value, Class<E> type) {
        if (value == null) {
            return null;
        }
        return Enum.valueOf(type, String.valueOf(value));
    }
}