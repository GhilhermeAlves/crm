package com.becommerce.crm.automation.ai.application.action;

import com.becommerce.crm.sales.activity.application.dto.CreateActivityRequest;
import com.becommerce.crm.sales.activity.application.port.in.ActivityUseCase;
import com.becommerce.crm.sales.pipeline.application.dto.UpdateOpportunityRequest;
import com.becommerce.crm.sales.pipeline.application.port.in.OpportunityUseCase;
import com.becommerce.crm.sales.task.application.dto.CreateTaskRequest;
import com.becommerce.crm.sales.task.application.port.in.TaskUseCase;
import com.becommerce.crm.sales.activity.domain.ActivityType;
import com.becommerce.crm.automation.ai.domain.AiAction;
import com.becommerce.crm.sales.task.domain.TaskPriority;
import com.becommerce.crm.sales.scheduling.application.dto.RescheduleRequest;
import com.becommerce.crm.sales.scheduling.application.port.in.AppointmentUseCase;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
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
    private final AppointmentUseCase appointmentUseCase;

    public AiActionExecutor(TaskUseCase taskUseCase,
                            ActivityUseCase activityUseCase,
                            OpportunityUseCase opportunityUseCase,
                            AppointmentUseCase appointmentUseCase) {
        this.taskUseCase = taskUseCase;
        this.activityUseCase = activityUseCase;
        this.opportunityUseCase = opportunityUseCase;
        this.appointmentUseCase = appointmentUseCase;
    }

    /**
     * Transação própria: se o service de domínio falhar (conflito de horário,
     * registro inexistente), só esta escrita é desfeita e o {@code AiActionService}
     * consegue gravar a ação como FAILED em vez de estourar UnexpectedRollbackException.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
            case "cancel_appointment" -> appointmentUseCase.cancel(
                    action.getCompanyId(), uuid(p.get("appointmentId")), str(p.get("reason")));
            case "reschedule_appointment" -> appointmentUseCase.reschedule(
                    action.getCompanyId(), uuid(p.get("appointmentId")),
                    new RescheduleRequest(Instant.parse(str(p.get("startAt"))),
                            Instant.parse(str(p.get("endAt"))), false));
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