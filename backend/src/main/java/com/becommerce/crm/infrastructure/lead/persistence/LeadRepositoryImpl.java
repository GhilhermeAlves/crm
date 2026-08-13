package com.becommerce.crm.infrastructure.lead.persistence;

import com.becommerce.crm.application.lead.port.output.LeadRepository;
import com.becommerce.crm.domain.lead.Lead;
import com.becommerce.crm.domain.lead.LeadClassification;
import com.becommerce.crm.domain.lead.LeadSource;
import com.becommerce.crm.domain.lead.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class LeadRepositoryImpl implements LeadRepository {

    private final LeadJpaRepository jpaRepository;

    public LeadRepositoryImpl(LeadJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Lead save(Lead lead) {
        return toDomain(jpaRepository.save(toEntity(lead)));
    }

    @Override
    public Optional<Lead> findById(UUID id) {
        return jpaRepository.findById(id).map(LeadRepositoryImpl::toDomain);
    }

    @Override
    public void delete(Lead lead) {
        jpaRepository.deleteById(lead.getId());
    }

    @Override
    public boolean existsByContactIdAndCompanyId(UUID contactId, UUID companyId) {
        return jpaRepository.existsByContactIdAndCompanyId(contactId, companyId);
    }

    @Override
    public PageResult findByCompanyWithFilters(UUID companyId, String status, String source, String classification,
                                               int page, int pageSize, String sortBy, String sortDirection) {
        Sort sort = Sort.by("desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy != null ? sortBy : "createdAt");
        PageRequest pageRequest = PageRequest.of(page, pageSize, sort);

        Page<LeadJpaEntity> result = jpaRepository.findByCompanyWithFilters(
                companyId, status, source, classification, pageRequest);

        List<Lead> leads = result.getContent().stream()
                .map(LeadRepositoryImpl::toDomain)
                .toList();

        return new PageResult(leads, result.getTotalElements());
    }

    private static LeadJpaEntity toEntity(Lead l) {
        LeadJpaEntity e = new LeadJpaEntity();
        e.setId(l.getId());
        e.setCompanyId(l.getCompanyId());
        e.setContactId(l.getContactId());
        e.setStatus(l.getStatus() != null ? l.getStatus().name() : null);
        e.setScore(l.getScore());
        e.setClassification(l.getClassification() != null ? l.getClassification().name() : null);
        e.setSource(l.getSource() != null ? l.getSource().name() : null);
        e.setCampaignId(l.getCampaignId());
        e.setAssignedTo(l.getAssignedTo());
        e.setNotes(l.getNotes());
        e.setCreatedAt(l.getCreatedAt());
        e.setUpdatedAt(l.getUpdatedAt());
        return e;
    }

    private static Lead toDomain(LeadJpaEntity e) {
        return Lead.reconstitute(
                e.getId(), e.getCompanyId(), e.getContactId(),
                e.getStatus() != null ? LeadStatus.valueOf(e.getStatus()) : LeadStatus.NEW,
                e.getScore(),
                e.getClassification() != null ? LeadClassification.valueOf(e.getClassification()) : null,
                e.getSource() != null ? LeadSource.valueOf(e.getSource()) : null,
                e.getCampaignId(), e.getAssignedTo(), e.getNotes(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}