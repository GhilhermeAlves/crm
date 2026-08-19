package com.becommerce.crm.application.ai.context;

import com.becommerce.crm.application.contact.port.output.ContactRepository;
import com.becommerce.crm.application.task.port.output.TaskRepository;
import com.becommerce.crm.domain.ai.AiRecordType;
import com.becommerce.crm.domain.contact.Contact;
import com.becommerce.crm.domain.task.Task;
import com.becommerce.crm.domain.task.TaskPriority;
import com.becommerce.crm.domain.task.TaskStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolver de contexto para {@code TASK} (AI-02). Monta o contexto da tarefa em
 * foco: título, descrição, status, prioridade, prazo e contato vinculado. Exige
 * {@code task:read}.
 */
@Component
public class TaskContextResolver implements AiRecordContextResolver {

    public static final String PERMISSION = "task:read";

    private final TaskRepository taskRepository;
    private final ContactRepository contactRepository;

    public TaskContextResolver(TaskRepository taskRepository, ContactRepository contactRepository) {
        this.taskRepository = taskRepository;
        this.contactRepository = contactRepository;
    }

    @Override
    public AiRecordType type() {
        return AiRecordType.TASK;
    }

    @Override
    public String requiredPermission() {
        return PERMISSION;
    }

    @Override
    public String resolve(UUID companyId, UUID recordId) {
        Optional<Task> maybe = taskRepository.findById(recordId);
        if (maybe.isEmpty()) {
            return null;
        }
        Task t = maybe.get();
        StringBuilder sb = new StringBuilder();
        sb.append("Tarefa: ").append(t.getTitle()).append('\n');
        if (t.getDescription() != null && !t.getDescription().isBlank()) {
            sb.append("Descrição: ").append(t.getDescription()).append('\n');
        }
        sb.append("Status: ").append(statusLabel(t.getStatus())).append('\n');
        sb.append("Prioridade: ").append(priorityLabel(t.getPriority())).append('\n');
        if (t.getDueAt() != null) {
            sb.append("Prazo: ").append(t.getDueAt().toLocalDate()).append('\n');
        }
        if (t.getContactId() != null) {
            contactRepository.findById(t.getContactId()).ifPresent(c ->
                    sb.append("Contato vinculado: ").append(fullName(c)).append('\n'));
        }
        return sb.toString();
    }

    private static String statusLabel(TaskStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case PENDING -> "PENDENTE";
            case IN_PROGRESS -> "EM ANDAMENTO";
            case COMPLETED -> "CONCLUÍDA";
            case CANCELLED -> "CANCELADA";
        };
    }

    private static String priorityLabel(TaskPriority priority) {
        if (priority == null) {
            return "";
        }
        return switch (priority) {
            case LOW -> "BAIXA";
            case MEDIUM -> "MÉDIA";
            case HIGH -> "ALTA";
        };
    }

    private static String fullName(Contact c) {
        return (c.getFirstName() + " " + (c.getLastName() == null ? "" : c.getLastName())).trim();
    }
}