package com.becommerce.crm.presentation.rest.pipeline;

import com.becommerce.crm.application.pipeline.dto.CreatePipelineRequest;
import com.becommerce.crm.application.pipeline.dto.CreateStageRequest;
import com.becommerce.crm.application.pipeline.dto.PipelineMetricsResponse;
import com.becommerce.crm.application.pipeline.dto.PipelineResponse;
import com.becommerce.crm.application.pipeline.dto.ReorderStagesRequest;
import com.becommerce.crm.application.pipeline.dto.UpdatePipelineRequest;
import com.becommerce.crm.application.pipeline.dto.UpdateStageRequest;
import com.becommerce.crm.application.pipeline.port.input.PipelineUseCase;
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
 * Pipelines e estágios (Sprint 11). Acesso restrito à própria empresa; RLS e a
 * checagem central de tenant ({@code @CurrentCompanyId}) + {@code TenantContext}
 * no serviço garantem isolamento multi-tenant real.
 */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/pipelines")
public class PipelineController {

    private final PipelineUseCase pipelineUseCase;

    public PipelineController(PipelineUseCase pipelineUseCase) {
        this.pipelineUseCase = pipelineUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('pipeline:view')")
    public ResponseEntity<List<PipelineResponse>> list(
            @CurrentCompanyId("Você só pode acessar pipelines da sua própria empresa.") UUID companyId) {
        return ResponseEntity.ok(pipelineUseCase.list(companyId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('pipeline:update')")
    public ResponseEntity<PipelineResponse> create(
            @CurrentCompanyId("Você só pode acessar pipelines da sua própria empresa.") UUID companyId,
            @Valid @RequestBody CreatePipelineRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        PipelineResponse response = pipelineUseCase.create(companyId, request, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{pipelineId}")
    @PreAuthorize("hasAuthority('pipeline:view')")
    public ResponseEntity<PipelineResponse> getById(
            @CurrentCompanyId("Você só pode acessar pipelines da sua própria empresa.") UUID companyId,
            @PathVariable UUID pipelineId) {
        return ResponseEntity.ok(pipelineUseCase.getById(companyId, pipelineId));
    }

    @PutMapping("/{pipelineId}")
    @PreAuthorize("hasAuthority('pipeline:update')")
    public ResponseEntity<PipelineResponse> update(
            @CurrentCompanyId("Você só pode acessar pipelines da sua própria empresa.") UUID companyId,
            @PathVariable UUID pipelineId,
            @Valid @RequestBody UpdatePipelineRequest request) {
        return ResponseEntity.ok(pipelineUseCase.update(companyId, pipelineId, request));
    }

    @DeleteMapping("/{pipelineId}")
    @PreAuthorize("hasAuthority('pipeline:update')")
    public ResponseEntity<Void> delete(
            @CurrentCompanyId("Você só pode acessar pipelines da sua própria empresa.") UUID companyId,
            @PathVariable UUID pipelineId) {
        pipelineUseCase.delete(companyId, pipelineId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{pipelineId}/stages")
    @PreAuthorize("hasAuthority('pipeline:update')")
    public ResponseEntity<PipelineResponse> addStage(
            @CurrentCompanyId("Você só pode acessar pipelines da sua própria empresa.") UUID companyId,
            @PathVariable UUID pipelineId,
            @Valid @RequestBody CreateStageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pipelineUseCase.addStage(companyId, pipelineId, request));
    }

    @PutMapping("/{pipelineId}/stages/{stageId}")
    @PreAuthorize("hasAuthority('pipeline:update')")
    public ResponseEntity<PipelineResponse> updateStage(
            @CurrentCompanyId("Você só pode acessar pipelines da sua própria empresa.") UUID companyId,
            @PathVariable UUID pipelineId,
            @PathVariable UUID stageId,
            @Valid @RequestBody UpdateStageRequest request) {
        return ResponseEntity.ok(pipelineUseCase.updateStage(companyId, pipelineId, stageId, request));
    }

    @PutMapping("/{pipelineId}/stages/reorder")
    @PreAuthorize("hasAuthority('pipeline:update')")
    public ResponseEntity<PipelineResponse> reorderStages(
            @CurrentCompanyId("Você só pode acessar pipelines da sua própria empresa.") UUID companyId,
            @PathVariable UUID pipelineId,
            @Valid @RequestBody ReorderStagesRequest request) {
        return ResponseEntity.ok(pipelineUseCase.reorderStages(companyId, pipelineId, request));
    }

    @GetMapping("/{pipelineId}/metrics")
    @PreAuthorize("hasAuthority('pipeline:view')")
    public ResponseEntity<PipelineMetricsResponse> metrics(
            @CurrentCompanyId("Você só pode acessar pipelines da sua própria empresa.") UUID companyId,
            @PathVariable UUID pipelineId) {
        return ResponseEntity.ok(pipelineUseCase.metrics(companyId, pipelineId));
    }
}