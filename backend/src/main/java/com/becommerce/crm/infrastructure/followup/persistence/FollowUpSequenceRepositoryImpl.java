package com.becommerce.crm.infrastructure.followup.persistence;

import com.becommerce.crm.application.followup.port.output.FollowUpSequenceRepository;
import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.domain.followup.FollowUpSequence;
import com.becommerce.crm.domain.followup.FollowUpSequenceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public class FollowUpSequenceRepositoryImpl implements FollowUpSequenceRepository {

    private final FollowUpSequenceJpaRepository jpaRepository;

    public FollowUpSequenceRepositoryImpl(FollowUpSequenceJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public FollowUpSequence save(FollowUpSequence sequence) {
        return toDomain(jpaRepository.save(toEntity(sequence)));
    }

    @Override
    public Optional<FollowUpSequence> findById(UUID id) {
        return jpaRepository.findById(id).map(FollowUpSequenceRepositoryImpl::toDomain);
    }

    @Override
    public PageResponse<FollowUpSequence> findByCompany(UUID companyId, int page, int pageSize) {
        Page<FollowUpSequenceJpaEntity> result = jpaRepository.findByCompany(companyId, PageRequest.of(page, pageSize));
        return PageResponse.of(result.stream().map(FollowUpSequenceRepositoryImpl::toDomain).toList(),
                page, pageSize, result.getTotalElements());
    }

    @Override
    @Transactional
    public void delete(UUID companyId, UUID sequenceId) {
        jpaRepository.deleteByIdAndCompany(sequenceId, companyId);
    }

    private static FollowUpSequenceJpaEntity toEntity(FollowUpSequence s) {
        FollowUpSequenceJpaEntity e = new FollowUpSequenceJpaEntity();
        e.setId(s.getId());
        e.setCompanyId(s.getCompanyId());
        e.setName(s.getName());
        e.setDescription(s.getDescription());
        if (s.getStatus() != null) { e.setStatus(s.getStatus().name()); }
        e.setCreatedAt(s.getCreatedAt());
        e.setUpdatedAt(s.getUpdatedAt());
        return e;
    }

    private static FollowUpSequence toDomain(FollowUpSequenceJpaEntity e) {
        return FollowUpSequence.reconstitute(e.getId(), e.getCompanyId(), e.getName(), e.getDescription(),
                e.getStatus() == null ? null : FollowUpSequenceStatus.valueOf(e.getStatus()),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}