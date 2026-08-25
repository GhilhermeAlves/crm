package com.becommerce.crm.infrastructure.campaign.persistence;

import com.becommerce.crm.application.campaign.port.output.CampaignExecutionRepository;
import com.becommerce.crm.domain.campaign.CampaignExecution;
import com.becommerce.crm.domain.campaign.ExecutionStatus;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class CampaignExecutionRepositoryImpl implements CampaignExecutionRepository {

    private final CampaignExecutionJpaRepository jpaRepository;

    public CampaignExecutionRepositoryImpl(CampaignExecutionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public CampaignExecution save(CampaignExecution execution) {
        return toDomain(jpaRepository.save(toEntity(execution)));
    }

    @Override
    public Optional<CampaignExecution> findById(UUID id) {
        return jpaRepository.findById(id).map(CampaignExecutionRepositoryImpl::toDomain);
    }

    @Override
    public Optional<CampaignExecution> findLatestByCampaignId(UUID campaignId) {
        return jpaRepository.findFirstByCampaignIdOrderByCreatedAtDesc(campaignId)
                .map(CampaignExecutionRepositoryImpl::toDomain);
    }

    private static CampaignExecutionJpaEntity toEntity(CampaignExecution e) {
        CampaignExecutionJpaEntity entity = new CampaignExecutionJpaEntity();
        entity.setId(e.getId());
        entity.setCompanyId(e.getCompanyId());
        entity.setCampaignId(e.getCampaignId());
        entity.setStatus(e.getStatus() != null ? e.getStatus().name() : null);
        entity.setTemplateSnapshot(e.getTemplateSnapshot());
        entity.setTotalRecipients(e.getTotalRecipients());
        entity.setProcessedCount(e.getProcessedCount());
        entity.setFailedCount(e.getFailedCount());
        entity.setCursorOffset(e.getCursorOffset());
        entity.setStartedAt(e.getStartedAt());
        entity.setFinishedAt(e.getFinishedAt());
        entity.setCreatedAt(e.getCreatedAt());
        return entity;
    }

    private static CampaignExecution toDomain(CampaignExecutionJpaEntity e) {
        return CampaignExecution.reconstitute(
                e.getId(), e.getCompanyId(), e.getCampaignId(),
                e.getStatus() != null ? ExecutionStatus.valueOf(e.getStatus()) : ExecutionStatus.RUNNING,
                e.getTemplateSnapshot(), e.getTotalRecipients(), e.getProcessedCount(),
                e.getFailedCount(), e.getCursorOffset(), e.getStartedAt(), e.getFinishedAt(),
                e.getCreatedAt());
    }
}
