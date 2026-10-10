package com.becommerce.crm.masterdata.anamnesis.infrastructure.persistence;

import com.becommerce.crm.masterdata.anamnesis.application.port.output.AnamnesisSectionRepository;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisSection;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class AnamnesisSectionRepositoryImpl implements AnamnesisSectionRepository {

    private final AnamnesisSectionJpaRepository jpaRepository;

    public AnamnesisSectionRepositoryImpl(AnamnesisSectionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AnamnesisSection save(AnamnesisSection section) {
        return toDomain(jpaRepository.save(toEntity(section)));
    }

    @Override
    public List<AnamnesisSection> findByModelIdOrderBySortOrder(UUID modelId) {
        return jpaRepository.findByModelIdOrderBySortOrderAsc(modelId).stream()
                .map(AnamnesisSectionRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public void deleteByModelId(UUID modelId) {
        jpaRepository.deleteByModelId(modelId);
    }

    static AnamnesisSection toDomain(AnamnesisSectionJpaEntity e) {
        return AnamnesisSection.reconstitute(e.getId(), e.getModelId(), e.getCompanyId(), e.getTitle(),
                e.isProfessional(), e.getSortOrder(), e.getCreatedAt(), e.getUpdatedAt());
    }

    static AnamnesisSectionJpaEntity toEntity(AnamnesisSection s) {
        AnamnesisSectionJpaEntity e = new AnamnesisSectionJpaEntity();
        e.setId(s.getId());
        e.setModelId(s.getModelId());
        e.setCompanyId(s.getCompanyId());
        e.setTitle(s.getTitle());
        e.setProfessional(s.isProfessional());
        e.setSortOrder(s.getSortOrder());
        e.setCreatedAt(s.getCreatedAt());
        e.setUpdatedAt(s.getUpdatedAt());
        return e;
    }
}
