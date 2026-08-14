package com.becommerce.crm.presentation.rest.workflow;

import com.becommerce.crm.application.workflow.dto.CreateWorkflowRequest;
import com.becommerce.crm.application.workflow.dto.UpdateWorkflowRequest;
import com.becommerce.crm.application.workflow.dto.WorkflowExecutionResponse;
import com.becommerce.crm.application.workflow.dto.WorkflowResponse;
import com.becommerce.crm.application.workflow.port.input.WorkflowUseCase;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
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
 * Workflows (Sprint 14). CRUD scoped à empresa ativa; ativação/desativação via
 * endpoints dedicados; histórico de execução (Item 7). Nenhuma lógica de
 * execução aqui — o executor roda automaticamente ao consumir eventos.
 */
@RestController
public class WorkflowController {

    private final WorkflowUseCase workflowUseCase;

    public WorkflowController(WorkflowUseCase workflowUseCase) {
        this.workflowUseCase = workflowUseCase;
    }

    @PostMapping("/api/v1/companies/{companyId}/workflows")
    @PreAuthorize("hasAuthority('workflow:create')")
    public ResponseEntity<WorkflowResponse> create(
            @PathVariable UUID companyId,
            @Valid @RequestBody CreateWorkflowRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowUseCase.create(companyId, request));
    }

    @GetMapping("/api/v1/companies/{companyId}/workflows")
    @PreAuthorize("hasAuthority('workflow:read')")
    public ResponseEntity<List<WorkflowResponse>> list(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(workflowUseCase.listByCompany(companyId));
    }

    @GetMapping("/api/v1/companies/{companyId}/workflows/{workflowId}")
    @PreAuthorize("hasAuthority('workflow:read')")
    public ResponseEntity<WorkflowResponse> getById(
            @PathVariable UUID companyId,
            @PathVariable UUID workflowId,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(workflowUseCase.getById(companyId, workflowId));
    }

    @PutMapping("/api/v1/companies/{companyId}/workflows/{workflowId}")
    @PreAuthorize("hasAuthority('workflow:update')")
    public ResponseEntity<WorkflowResponse> update(
            @PathVariable UUID companyId,
            @PathVariable UUID workflowId,
            @Valid @RequestBody UpdateWorkflowRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(workflowUseCase.update(companyId, workflowId, request));
    }

    @PostMapping("/api/v1/companies/{companyId}/workflows/{workflowId}/activate")
    @PreAuthorize("hasAuthority('workflow:update')")
    public ResponseEntity<WorkflowResponse> activate(
            @PathVariable UUID companyId,
            @PathVariable UUID workflowId,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(workflowUseCase.activate(companyId, workflowId));
    }

    @PostMapping("/api/v1/companies/{companyId}/workflows/{workflowId}/deactivate")
    @PreAuthorize("hasAuthority('workflow:update')")
    public ResponseEntity<WorkflowResponse> deactivate(
            @PathVariable UUID companyId,
            @PathVariable UUID workflowId,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(workflowUseCase.deactivate(companyId, workflowId));
    }

    @DeleteMapping("/api/v1/companies/{companyId}/workflows/{workflowId}")
    @PreAuthorize("hasAuthority('workflow:delete')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID companyId,
            @PathVariable UUID workflowId,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        workflowUseCase.delete(companyId, workflowId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/companies/{companyId}/workflows/{workflowId}/executions")
    @PreAuthorize("hasAuthority('workflow:read')")
    public ResponseEntity<List<WorkflowExecutionResponse>> executions(
            @PathVariable UUID companyId,
            @PathVariable UUID workflowId,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(workflowUseCase.listExecutions(companyId, workflowId));
    }

    @GetMapping("/api/v1/companies/{companyId}/workflow-executions/recent")
    @PreAuthorize("hasAuthority('workflow:read')")
    public ResponseEntity<List<WorkflowExecutionResponse>> recentExecutions(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompanyAccess(companyId, principal);
        return ResponseEntity.ok(workflowUseCase.listRecentExecutions(companyId));
    }

    private void requireCompanyAccess(UUID companyId, CurrentUser principal) {
        boolean superAdmin = principal.roles().contains("SUPER_ADMIN");
        if (!superAdmin && !companyId.equals(principal.companyId())) {
            throw new CrmAccessDeniedException("Você só pode gerenciar workflows da sua própria empresa.");
        }
    }
}