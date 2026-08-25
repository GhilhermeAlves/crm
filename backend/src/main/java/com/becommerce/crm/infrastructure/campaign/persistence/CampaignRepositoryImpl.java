package com.becommerce.crm.infrastructure.campaign.persistence;

import com.becommerce.crm.application.campaign.port.output.CampaignRepository;
import com.becommerce.crm.domain.campaign.AudienceType;
import com.becommerce.crm.domain.campaign.Campaign;
import com.becommerce.crm.domain.campaign.CampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CampaignRepositoryImpl implements CampaignRepository {

    private final CampaignJpaRepository jpaRepository;

    public CampaignRepositoryImpl(CampaignJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Campaign save(Campaign campaign) {
        return toDomain(jpaRepository.save(toEntity(campaign)));
    }

    @Override
    public Optional<Campaign> findById(UUID id) {
        return jpaRepository.findById(id).map(CampaignRepositoryImpl::toDomain);
    }

    @Override
    public void delete(Campaign campaign) {
        jpaRepository.deleteById(campaign.getId());
    }

    @Override
    public PageResult findByCompanyWithFilters(UUID companyId, String status, String audienceType,
                                               int page, int pageSize) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Page<CampaignJpaEntity> result = jpaRepository.findByCompanyWithFilters(
                companyId, status, audienceType, PageRequest.of(page, pageSize, sort));
        List<Campaign> content = result.getContent().stream()
                .map(CampaignRepositoryImpl::toDomain).toList();
        return new PageResult(content, result.getTotalElements());
    }

    @Override
    @Transactional
    public boolean claimForExecution(UUID campaignId) {
        return jpaRepository.claimForExecution(campaignId) == 1;
    }

    @Override
    @Transactional
    public boolean resetToScheduled(UUID campaignId) {
        return jpaRepository.resetToScheduled(campaignId) == 1;
    }

    @Override
    @Transactional
    public boolean completeIfRunning(UUID campaignId) {
        return jpaRepository.completeIfRunning(campaignId) == 1;
    }

    @Override
    public List<Campaign> findDueForExecution(LocalDateTime now, int limit) {
        return jpaRepository.findDueForExecution(now, limit).stream()
                .map(CampaignRepositoryImpl::toDomain).toList();
    }

    private static CampaignJpaEntity toEntity(Campaign c) {
        CampaignJpaEntity e = new CampaignJpaEntity();
        e.setId(c.getId());
        e.setCompanyId(c.getCompanyId());
        e.setName(c.getName());
        e.setDescription(c.getDescription());
        e.setStatus(c.getStatus() != null ? c.getStatus().name() : CampaignStatus.DRAFT.name());
        e.setAudienceType(c.getAudienceType() != null ? c.getAudienceType().name() : null);
        e.setAudienceCriteria(c.getAudienceCriteria());
        e.setEstimatedRecipients(c.getEstimatedRecipients());
        e.setScheduledAt(c.getScheduledAt());
        e.setTimezone(c.getTimezone());
        e.setStartedAt(c.getStartedAt());
        e.setCompletedAt(c.getCompletedAt());
        e.setCreatedBy(c.getCreatedBy());
        e.setCreatedAt(c.getCreatedAt());
        e.setUpdatedAt(c.getUpdatedAt());
        return e;
    }

    private static Campaign toDomain(CampaignJpaEntity e) {
        return Campaign.reconstitute(
                e.getId(), e.getCompanyId(), e.getName(), e.getDescription(),
                e.getStatus() != null ? CampaignStatus.valueOf(e.getStatus()) : CampaignStatus.DRAFT,
                e.getAudienceType() != null ? AudienceType.valueOf(e.getAudienceType()) : AudienceType.CONTACTS,
                e.getAudienceCriteria(), e.getEstimatedRecipients(), e.getScheduledAt(),
                e.getTimezone(), e.getStartedAt(), e.getCompletedAt(), e.getCreatedBy(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}
