package com.becommerce.crm.application.pipeline.port.output;

import com.becommerce.crm.domain.pipeline.Opportunity;
import com.becommerce.crm.domain.pipeline.OpportunityHistory;
import com.becommerce.crm.domain.pipeline.OpportunityStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OpportunityRepository {

    Opportunity save(Opportunity opportunity);

    Optional<Opportunity> findById(UUID id);

    List<Opportunity> findByPipelineId(UUID pipelineId);

    List<Opportunity> findByCompanyId(UUID companyId);

    /** Oportunidades associadas a um contato (qualquer estado). */
    List<Opportunity> findByContactId(UUID contactId);

    List<Opportunity> findByPipelineIdAndStatus(UUID pipelineId, OpportunityStatus status);

    void delete(Opportunity opportunity);

    void saveHistory(OpportunityHistory history);

    List<OpportunityHistory> findHistoryByOpportunityId(UUID opportunityId);
}
