package com.becommerce.crm.application.workflow.service;

import com.becommerce.crm.application.activity.dto.CreateActivityRequest;
import com.becommerce.crm.application.activity.port.input.ActivityUseCase;
import com.becommerce.crm.application.task.dto.CreateTaskRequest;
import com.becommerce.crm.application.task.port.input.TaskUseCase;
import com.becommerce.crm.application.workflow.port.output.WorkflowExecutionRepository;
import com.becommerce.crm.domain.activity.ActivityType;
import com.becommerce.crm.domain.task.TaskPriority;
import com.becommerce.crm.domain.workflow.ActionType;
import com.becommerce.crm.domain.workflow.ExecutionStatus;
import com.becommerce.crm.domain.workflow.Workflow;
import com.becommerce.crm.domain.workflow.WorkflowAction;
import com.becommerce.crm.domain.workflow.event.WorkflowTriggerEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Executa uma única ação de workflow em uma transação própria
 * ({@code REQUIRES_NEW}) (Item 5/Item 7).
 *
 * <p>Isolamento transacional: falha de uma ação NUNCA contamina a transação do
 * evento originador. A idempotência (Item 6) é aplicada via
 * {@link WorkflowExecutionRepository#insertNew} (chave company/action/event):
 * se a mesma ação já foi registrada para o evento, a execução é pulada. Em caso
 * de erro, a exceção propaga para a transação ser revertida (nada parcial é
 * persistido) e o {@link WorkflowExecutor} registra a falha numa nova transação.
 */
@Component
public class WorkflowActionRunner {

    private static final Logger log = LoggerFactory.getLogger(WorkflowActionRunner.class);

    /** Ator de sistema usado para ações criadas por automação (não é um usuário real). */
    public static final UUID SYSTEM_ACTOR = UUID.fromString("00000000-0000-0000-0000-00000000b0b0");

    private final WorkflowExecutionRepository executionRepository;
    private final TaskUseCase taskUseCase;
    private final ActivityUseCase activityUseCase;
    private final ObjectMapper objectMapper;

    public WorkflowActionRunner(WorkflowExecutionRepository executionRepository,
                                TaskUseCase taskUseCase,
                                ActivityUseCase activityUseCase,
                                ObjectMapper objectMapper) {
        this.executionRepository = executionRepository;
        this.taskUseCase = taskUseCase;
        this.activityUseCase = activityUseCase;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void run(Workflow workflow, WorkflowAction action, WorkflowTriggerEvent event) {
        UUID executionId = UUID.randomUUID();
        int inserted = executionRepository.insertNew(executionId, event.companyId(), workflow.getId(),
                action.getId(), event.eventId(), event.trigger().name(), entityId(event), action.getActionType());
        if (inserted == 0) {
            return; // já processado (idempotência) — SKIP
        }
        String result = execute(workflow, action, event);
        executionRepository.updateResult(executionId, event.companyId(), ExecutionStatus.SUCCESS, result, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Workflow workflow, WorkflowAction action, WorkflowTriggerEvent event, Throwable error) {
        UUID executionId = UUID.randomUUID();
        int inserted = executionRepository.insertNew(executionId, event.companyId(), workflow.getId(),
                action.getId(), event.eventId(), event.trigger().name(), entityId(event), action.getActionType());
        if (inserted == 0) {
            return;
        }
        String message = error.getMessage() != null && !error.getMessage().isBlank()
                ? error.getMessage()
                : error.getClass().getSimpleName();
        if (message.length() > 2000) {
            message = message.substring(0, 2000);
        }
        executionRepository.updateResult(executionId, event.companyId(), ExecutionStatus.FAILED, null, message);
        log.warn("Workflow action FAILED: workflow={}, action={}, error={}",
                workflow.getName(), action.getActionType(), message);
    }

    private String execute(Workflow workflow, WorkflowAction action, WorkflowTriggerEvent event) {
        return switch (action.getActionType()) {
            case CREATE_TASK -> executeCreateTask(workflow, action.getConfig(), event);
            case CREATE_ACTIVITY -> executeCreateActivity(workflow, action.getConfig(), event);
        };
    }

    private String executeCreateTask(Workflow workflow, String configJson, WorkflowTriggerEvent event) {
        JsonNode cfg = parseConfig(configJson);
        String title = text(cfg, "title");
        if (title == null) {
            title = "Follow-up";
        }
        String description = text(cfg, "description");
        int dueInDays = cfg.path("dueInDays").asInt(0);
        TaskPriority priority = parsePriority(cfg.path("priority").asText(null));

        LocalDateTime dueAt = dueInDays > 0 ? LocalDateTime.now().plusDays(dueInDays) : null;
        String withAttribution = appendAttribution(description, workflow.getName());

        var response = taskUseCase.create(event.companyId(),
                new CreateTaskRequest(event.contactId(), event.opportunityId(), title,
                        withAttribution, null, dueAt, priority),
                SYSTEM_ACTOR);
        return "Task criada: " + response.id();
    }

    private String executeCreateActivity(Workflow workflow, String configJson, WorkflowTriggerEvent event) {
        JsonNode cfg = parseConfig(configJson);
        String subject = text(cfg, "subject");
        if (subject == null) {
            subject = "Follow-up automático";
        }
        String description = text(cfg, "description");
        ActivityType type = parseActivityType(cfg.path("type").asText(null));

        String withAttribution = appendAttribution(description, workflow.getName());
        var response = activityUseCase.create(event.companyId(),
                new CreateActivityRequest(event.contactId(), event.opportunityId(), type, subject,
                        withAttribution, LocalDateTime.now()),
                SYSTEM_ACTOR);
        return "Activity criada: " + response.id();
    }

    private JsonNode parseConfig(String configJson) {
        try {
            return objectMapper.readTree(configJson == null ? "{}" : configJson);
        } catch (Exception e) {
            throw new IllegalStateException("Configuração de ação inválida: " + configJson, e);
        }
    }

    private String text(JsonNode cfg, String field) {
        JsonNode node = cfg.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private TaskPriority parsePriority(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return TaskPriority.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private ActivityType parseActivityType(String value) {
        if (value == null || value.isBlank()) {
            return ActivityType.OTHER;
        }
        try {
            return ActivityType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ActivityType.OTHER;
        }
    }

    private String appendAttribution(String description, String workflowName) {
        String note = "Criada automaticamente pelo workflow: \"" + workflowName + "\"";
        if (description == null || description.isBlank()) {
            return note;
        }
        return description + "\n\n" + note;
    }

    private static UUID entityId(WorkflowTriggerEvent event) {
        if (event.opportunityId() != null) {
            return event.opportunityId();
        }
        if (event.taskId() != null) {
            return event.taskId();
        }
        if (event.activityId() != null) {
            return event.activityId();
        }
        return event.contactId();
    }
}