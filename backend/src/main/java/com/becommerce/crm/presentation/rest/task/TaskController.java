package com.becommerce.crm.presentation.rest.task;

import com.becommerce.crm.application.task.dto.CreateTaskRequest;
import com.becommerce.crm.application.task.dto.TaskResponse;
import com.becommerce.crm.application.task.dto.UpdateTaskRequest;
import com.becommerce.crm.application.task.port.input.TaskUseCase;
import com.becommerce.crm.domain.task.TaskStatus;
import com.becommerce.crm.infrastructure.security.config.CurrentCompanyId;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Tasks/Follow-up (Sprint 12). Crud scoped à empresa ativa; transições de
 * estado via POST /status/{status}. Relacionamentos (contact/opportunity)
 * opcionais e validados pelo serviço.
 */
@RestController
public class TaskController {

    private final TaskUseCase taskUseCase;

    public TaskController(TaskUseCase taskUseCase) {
        this.taskUseCase = taskUseCase;
    }

    @PostMapping("/api/v1/companies/{companyId}/tasks")
    @PreAuthorize("hasAuthority('task:create')")
    public ResponseEntity<TaskResponse> create(
            @CurrentCompanyId("Você só pode acessar tarefas da sua própria empresa.") UUID companyId,
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        TaskResponse response = taskUseCase.create(companyId, request, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/companies/{companyId}/tasks")
    @PreAuthorize("hasAuthority('task:read')")
    public ResponseEntity<List<TaskResponse>> list(
            @CurrentCompanyId("Você só pode acessar tarefas da sua própria empresa.") UUID companyId,
            @RequestParam(required = false) TaskStatus status) {
        return ResponseEntity.ok(taskUseCase.listByCompany(companyId, status));
    }

    @GetMapping("/api/v1/companies/{companyId}/tasks/due-today")
    @PreAuthorize("hasAuthority('task:read')")
    public ResponseEntity<List<TaskResponse>> listDueToday(
            @CurrentCompanyId("Você só pode acessar tarefas da sua própria empresa.") UUID companyId) {
        return ResponseEntity.ok(taskUseCase.listDueToday(companyId));
    }

    @GetMapping("/api/v1/companies/{companyId}/opportunities/{opportunityId}/tasks")
    @PreAuthorize("hasAuthority('task:read')")
    public ResponseEntity<List<TaskResponse>> listByOpportunity(
            @CurrentCompanyId("Você só pode acessar tarefas da sua própria empresa.") UUID companyId,
            @PathVariable UUID opportunityId) {
        return ResponseEntity.ok(taskUseCase.listByOpportunity(companyId, opportunityId));
    }

    @GetMapping("/api/v1/companies/{companyId}/tasks/{taskId}")
    @PreAuthorize("hasAuthority('task:read')")
    public ResponseEntity<TaskResponse> getById(
            @CurrentCompanyId("Você só pode acessar tarefas da sua própria empresa.") UUID companyId,
            @PathVariable UUID taskId) {
        return ResponseEntity.ok(taskUseCase.getById(companyId, taskId));
    }

    @PutMapping("/api/v1/companies/{companyId}/tasks/{taskId}")
    @PreAuthorize("hasAuthority('task:update')")
    public ResponseEntity<TaskResponse> update(
            @CurrentCompanyId("Você só pode acessar tarefas da sua própria empresa.") UUID companyId,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        return ResponseEntity.ok(taskUseCase.update(companyId, taskId, request));
    }

    @PostMapping("/api/v1/companies/{companyId}/tasks/{taskId}/status/{status}")
    @PreAuthorize("hasAuthority('task:update')")
    public ResponseEntity<TaskResponse> changeStatus(
            @CurrentCompanyId("Você só pode acessar tarefas da sua própria empresa.") UUID companyId,
            @PathVariable UUID taskId,
            @PathVariable TaskStatus status) {
        return ResponseEntity.ok(taskUseCase.changeStatus(companyId, taskId, status));
    }

    @DeleteMapping("/api/v1/companies/{companyId}/tasks/{taskId}")
    @PreAuthorize("hasAuthority('task:delete')")
    public ResponseEntity<Void> delete(
            @CurrentCompanyId("Você só pode acessar tarefas da sua própria empresa.") UUID companyId,
            @PathVariable UUID taskId) {
        taskUseCase.delete(companyId, taskId);
        return ResponseEntity.noContent().build();
    }
}