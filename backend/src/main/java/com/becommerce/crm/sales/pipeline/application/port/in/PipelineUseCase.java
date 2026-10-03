package com.becommerce.crm.sales.pipeline.application.port.in;

import com.becommerce.crm.sales.pipeline.application.dto.CreatePipelineRequest;
import com.becommerce.crm.sales.pipeline.application.dto.CreateStageRequest;
import com.becommerce.crm.sales.pipeline.application.dto.PipelineMetricsResponse;
import com.becommerce.crm.sales.pipeline.application.dto.PipelineResponse;
import com.becommerce.crm.sales.pipeline.application.dto.ReorderStagesRequest;
import com.becommerce.crm.sales.pipeline.application.dto.UpdatePipelineRequest;
import com.becommerce.crm.sales.pipeline.application.dto.UpdateStageRequest;

import java.util.List;
import java.util.UUID;

/** Casos de uso de pipelines (Sprint 11), sempre isolados pela empresa ativa. */
public interface PipelineUseCase {

    PipelineResponse create(UUID companyId, CreatePipelineRequest request, UUID createdBy);

    PipelineResponse update(UUID companyId, UUID pipelineId, UpdatePipelineRequest request);

    PipelineResponse getById(UUID companyId, UUID pipelineId);

    List<PipelineResponse> list(UUID companyId);

    void delete(UUID companyId, UUID pipelineId);

    PipelineResponse addStage(UUID companyId, UUID pipelineId, CreateStageRequest request);

    PipelineResponse updateStage(UUID companyId, UUID pipelineId, UUID stageId, UpdateStageRequest request);

    PipelineResponse reorderStages(UUID companyId, UUID pipelineId, ReorderStagesRequest request);

    PipelineMetricsResponse metrics(UUID companyId, UUID pipelineId);
}
