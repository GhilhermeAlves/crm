package com.becommerce.crm.application.pipeline.port.output;

import com.becommerce.crm.domain.pipeline.Opportunity;
import com.becommerce.crm.domain.pipeline.OpportunityHistory;
import com.becommerce.crm.domain.pipeline.OpportunityStatus;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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

    /**
     * Histórico de estágio de cada oportunidade informada, em uma única consulta
     * em lote (evita N+1 no Customer 360 / análise contextual).
     */
    Map<UUID, List<OpportunityHistory>> findHistoryByOpportunityIds(Collection<UUID> opportunityIds);
}
