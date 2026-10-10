package com.becommerce.crm.masterdata.anamnesis.infrastructure.persistence;

import com.becommerce.crm.masterdata.anamnesis.application.port.output.AnamnesisQuestionOptionRepository;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisQuestionOption;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class AnamnesisQuestionOptionRepositoryImpl implements AnamnesisQuestionOptionRepository {

    private final AnamnesisQuestionOptionJpaRepository jpaRepository;

    public AnamnesisQuestionOptionRepositoryImpl(AnamnesisQuestionOptionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AnamnesisQuestionOption save(AnamnesisQuestionOption option) {
        return toDomain(jpaRepository.save(toEntity(option)));
    }

    @Override
    public List<AnamnesisQuestionOption> findByModelIdOrderBySortOrder(UUID modelId) {
        return jpaRepository.findByModelIdOrderBySortOrderAsc(modelId).stream()
                .map(AnamnesisQuestionOptionRepositoryImpl::toDomain)
                .toList();
    }

    static AnamnesisQuestionOption toDomain(AnamnesisQuestionOptionJpaEntity e) {
        return AnamnesisQuestionOption.reconstitute(e.getId(), e.getQuestionId(), e.getCompanyId(),
                e.getLabel(), e.getValue(), e.getSortOrder(), e.getCreatedAt(), e.getUpdatedAt());
    }

    static AnamnesisQuestionOptionJpaEntity toEntity(AnamnesisQuestionOption o) {
        AnamnesisQuestionOptionJpaEntity e = new AnamnesisQuestionOptionJpaEntity();
        e.setId(o.getId());
        e.setQuestionId(o.getQuestionId());
        e.setCompanyId(o.getCompanyId());
        e.setLabel(o.getLabel());
        e.setValue(o.getValue());
        e.setSortOrder(o.getSortOrder());
        e.setCreatedAt(o.getCreatedAt());
        e.setUpdatedAt(o.getUpdatedAt());
        return e;
    }
}
