package com.becommerce.crm.application.task.service;

import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.application.contact.port.output.ContactRepository;
import com.becommerce.crm.application.pipeline.port.output.OpportunityRepository;
import com.becommerce.crm.application.task.dto.CreateTaskRequest;
import com.becommerce.crm.application.task.dto.UpdateTaskRequest;
import com.becommerce.crm.application.task.port.output.TaskRepository;
import com.becommerce.crm.domain.contact.Contact;
import com.becommerce.crm.domain.contact.exception.ContactNotFoundException;
import com.becommerce.crm.domain.task.Task;
import com.becommerce.crm.domain.task.TaskPriority;
import com.becommerce.crm.domain.task.TaskStatus;
import com.becommerce.crm.domain.task.exception.TaskNotFoundException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock ContactRepository contactRepository;
    @Mock OpportunityRepository opportunityRepository;
    @Mock TenantAuditRecorder auditor;
    @Mock com.becommerce.crm.application.identity.port.output.EventPublisher eventPublisher;

    @InjectMocks TaskService taskService;

    private final UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    private Contact ownedContact() {
        return Contact.reconstitute(UUID.randomUUID(), companyId, "Ana", "Souza", "ana@e.com",
                null, null, LocalDateTime.now(), LocalDateTime.now(), null);
    }

    private Task task(TaskStatus status) {
        return Task.reconstitute(UUID.randomUUID(), companyId, null, null, "Follow-up",
                null, null, LocalDateTime.now(), TaskPriority.HIGH, status, null,
                UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void shouldCreateTaskWithDefaultMediumPriority() {
        var response = taskService.create(companyId,
                new CreateTaskRequest(null, null, "Ligar", null, null, LocalDateTime.now(),
                        null), UUID.randomUUID());

        assertEquals(TaskStatus.PENDING, response.status());
        assertEquals(TaskPriority.MEDIUM, response.priority());
        verify(taskRepository).save(any(Task.class));
        assertNull(TenantContext.getCompanyId(), "contexto deve ser limpo");
    }

    @Test
    void shouldRejectForeignContact() {
        Contact foreign = Contact.reconstitute(UUID.randomUUID(), UUID.randomUUID(), "Ana", "Souza",
                "ana@e.com", null, null, LocalDateTime.now(), LocalDateTime.now(), null);
        when(contactRepository.findById(foreign.getId())).thenReturn(Optional.of(foreign));

        assertThrows(ContactNotFoundException.class, () -> taskService.create(companyId,
                new CreateTaskRequest(foreign.getId(), null, "X", null, null,
                        LocalDateTime.now(), null), UUID.randomUUID()));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void shouldRejectWhenTaskBelongsToAnotherCompany() {
        Task foreign = Task.reconstitute(UUID.randomUUID(), UUID.randomUUID(), null, null, "t",
                null, null, null, TaskPriority.MEDIUM, TaskStatus.PENDING, null,
                UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now());
        when(taskRepository.findById(foreign.getId())).thenReturn(Optional.of(foreign));

        assertThrows(TaskNotFoundException.class, () -> taskService.getById(companyId, foreign.getId()));
    }

    @Test
    void shouldCompleteTask() {
        Task t = task(TaskStatus.PENDING);
        when(taskRepository.findById(t.getId())).thenReturn(Optional.of(t));

        var response = taskService.changeStatus(companyId, t.getId(), TaskStatus.COMPLETED);

        assertEquals(TaskStatus.COMPLETED, response.status());
        assertNotNull(response.completedAt());
        verify(taskRepository).save(t);
    }

    @Test
    void shouldRejectUpdatingCompletedTask() {
        Task completed = task(TaskStatus.COMPLETED);
        when(taskRepository.findById(completed.getId())).thenReturn(Optional.of(completed));

        assertThrows(Exception.class, () -> taskService.update(companyId, completed.getId(),
                new UpdateTaskRequest("Novo", null, null, null, null, null, null)));
    }

    @Test
    void shouldListDueToday() {
        Task t = task(TaskStatus.PENDING);
        when(taskRepository.findDueToday(eq(companyId), any(), any())).thenReturn(List.of(t));

        assertEquals(1, taskService.listDueToday(companyId).size());
    }
}