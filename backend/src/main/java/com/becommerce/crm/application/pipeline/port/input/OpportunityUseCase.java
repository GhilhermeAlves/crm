package com.becommerce.crm.application.pipeline.port.input;

import com.becommerce.crm.application.pipeline.dto.CreateOpportunityRequest;
import com.becommerce.crm.application.pipeline.dto.MarkLostRequest;
import com.becommerce.crm.application.pipeline.dto.MoveOpportunityRequest;
import com.becommerce.crm.application.pipeline.dto.OpportunityHistoryResponse;
import com.becommerce.crm.application.pipeline.dto.OpportunityResponse;
import com.becommerce.crm.application.pipeline.dto.UpdateOpportunityRequest;
import com.becommerce.crm.domain.pipeline.OpportunityStatus;

import java.util.List;
import java.util.UUID;

/** Casos de uso de oportunidades (Sprint 11), isolados pela empresa ativa. */
public interface OpportunityUseCase {

    OpportunityResponse create(UUID companyId, UUID pipelineId, CreateOpportunityRequest request, UUID createdBy);

    OpportunityResponse getById(UUID companyId, UUID opportunityId);

    OpportunityResponse update(UUID companyId, UUID opportunityId, UpdateOpportunityRequest request, UUID changedBy);

    OpportunityResponse move(UUID companyId, UUID opportunityId, MoveOpportunityRequest request, UUID changedBy);

    OpportunityResponse markWon(UUID companyId, UUID opportunityId, UUID changedBy);

    OpportunityResponse markLost(UUID companyId, UUID opportunityId, MarkLostRequest request, UUID changedBy);

    void delete(UUID companyId, UUID opportunityId);

    List<OpportunityResponse> listByPipeline(UUID companyId, UUID pipelineId);

    List<OpportunityHistoryResponse> history(UUID companyId, UUID opportunityId);

    /** Busca oportunidades com filtros opcionais (para a IA), limitada e ordenada por atualização. */
    List<OpportunityResponse> search(UUID companyId, OpportunityStatus status, UUID pipelineId,
                                     UUID contactId, UUID stageId, UUID assignedTo, int limit);
}
