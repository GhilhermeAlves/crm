package com.becommerce.crm.infrastructure.pipeline.persistence;

import com.becommerce.crm.application.pipeline.port.output.OpportunityRepository;
import com.becommerce.crm.domain.pipeline.Opportunity;
import com.becommerce.crm.domain.pipeline.OpportunityHistory;
import com.becommerce.crm.domain.pipeline.OpportunityStatus;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class OpportunityRepositoryImpl implements OpportunityRepository {

    private final OpportunityJpaRepository jpaRepository;
    private final OpportunityHistoryJpaRepository historyJpaRepository;

    public OpportunityRepositoryImpl(OpportunityJpaRepository jpaRepository,
                                     OpportunityHistoryJpaRepository historyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.historyJpaRepository = historyJpaRepository;
    }

    @Override
    public Opportunity save(Opportunity opportunity) {
        return toDomain(jpaRepository.save(toEntity(opportunity)));
    }

    @Override
    public Optional<Opportunity> findById(UUID id) {
        return jpaRepository.findById(id).map(OpportunityRepositoryImpl::toDomain);
    }

    @Override
    public List<Opportunity> findByPipelineId(UUID pipelineId) {
        return jpaRepository.findByPipelineId(pipelineId).stream()
                .map(OpportunityRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public List<Opportunity> findByCompanyId(UUID companyId) {
        return jpaRepository.findByCompanyId(companyId).stream()
                .map(OpportunityRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public List<Opportunity> findByPipelineIdAndStatus(UUID pipelineId, OpportunityStatus status) {
        return jpaRepository.findByPipelineIdAndStatus(pipelineId, status == null ? null : status.name())
                .stream().map(OpportunityRepositoryImpl::toDomain).toList();
    }

    @Override
    public List<Opportunity> findByContactId(UUID contactId) {
        return jpaRepository.findByContactId(contactId).stream()
                .map(OpportunityRepositoryImpl::toDomain).toList();
    }

    @Override
    public void delete(Opportunity opportunity) {
        jpaRepository.deleteById(opportunity.getId());
    }

    @Override
    public void saveHistory(OpportunityHistory history) {
        historyJpaRepository.save(toHistoryEntity(history));
    }

    @Override
    public List<OpportunityHistory> findHistoryByOpportunityId(UUID opportunityId) {
        return historyJpaRepository.findByOpportunityIdOrderByChangedAtAsc(opportunityId).stream()
                .map(OpportunityRepositoryImpl::toHistoryDomain)
                .toList();
    }

    @Override
    public Map<UUID, List<OpportunityHistory>> findHistoryByOpportunityIds(Collection<UUID> opportunityIds) {
        if (opportunityIds == null || opportunityIds.isEmpty()) {
            return Map.of();
        }
        return historyJpaRepository.findByOpportunityIdInOrderByChangedAtAsc(opportunityIds).stream()
                .map(OpportunityRepositoryImpl::toHistoryDomain)
                .collect(Collectors.groupingBy(OpportunityHistory::getOpportunityId,
                        LinkedHashMap::new, Collectors.toList()));
    }

    private static OpportunityJpaEntity toEntity(Opportunity o) {
        OpportunityJpaEntity e = new OpportunityJpaEntity();
        e.setId(o.getId());
        e.setCompanyId(o.getCompanyId());
        e.setTitle(o.getTitle());
        e.setValue(o.getValue());
        e.setContactId(o.getContactId());
        e.setPipelineId(o.getPipelineId());
        e.setStageId(o.getStageId());
        e.setAssignedTo(o.getAssignedTo());
        e.setExpectedCloseDate(o.getExpectedCloseDate());
        e.setStatus(o.getStatus() != null ? o.getStatus().name() : null);
        e.setWonAt(o.getWonAt());
        e.setLostAt(o.getLostAt());
        e.setLossReason(o.getLossReason());
        e.setNotes(o.getNotes());
        e.setCreatedAt(o.getCreatedAt());
        e.setUpdatedAt(o.getUpdatedAt());
        return e;
    }

    private static Opportunity toDomain(OpportunityJpaEntity e) {
        return Opportunity.reconstitute(e.getId(), e.getCompanyId(), e.getTitle(), e.getValue(),
                e.getContactId(), e.getPipelineId(), e.getStageId(), e.getAssignedTo(),
                e.getExpectedCloseDate(),
                e.getStatus() != null ? OpportunityStatus.valueOf(e.getStatus()) : OpportunityStatus.OPEN,
                e.getWonAt(), e.getLostAt(), e.getLossReason(), e.getNotes(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    private static OpportunityHistoryJpaEntity toHistoryEntity(OpportunityHistory h) {
        OpportunityHistoryJpaEntity e = new OpportunityHistoryJpaEntity();
        e.setId(h.getId());
        e.setOpportunityId(h.getOpportunityId());
        e.setFromStageId(h.getFromStageId());
        e.setToStageId(h.getToStageId());
        e.setChangedBy(h.getChangedBy());
        e.setChangedAt(h.getChangedAt());
        e.setNote(h.getNote());
        return e;
    }

    private static OpportunityHistory toHistoryDomain(OpportunityHistoryJpaEntity e) {
        return OpportunityHistory.reconstitute(e.getId(), e.getOpportunityId(), e.getFromStageId(),
                e.getToStageId(), e.getChangedBy(), e.getChangedAt(), e.getNote());
    }
}
