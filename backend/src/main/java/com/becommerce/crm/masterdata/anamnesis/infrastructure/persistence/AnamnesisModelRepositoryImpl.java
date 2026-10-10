package com.becommerce.crm.masterdata.anamnesis.infrastructure.persistence;

import com.becommerce.crm.masterdata.anamnesis.application.port.output.AnamnesisModelRepository;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisModel;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AnamnesisModelRepositoryImpl implements AnamnesisModelRepository {

    private final AnamnesisModelJpaRepository jpaRepository;

    public AnamnesisModelRepositoryImpl(AnamnesisModelJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AnamnesisModel save(AnamnesisModel model) {
        return toDomain(jpaRepository.save(toEntity(model)));
    }

    @Override
    public Optional<AnamnesisModel> findById(UUID id) {
        return jpaRepository.findById(id).map(AnamnesisModelRepositoryImpl::toDomain);
    }

    @Override
    public List<AnamnesisModel> findByCompanyIdOrderByName(UUID companyId) {
        return jpaRepository.findByCompanyIdOrderByNameAsc(companyId).stream()
                .map(AnamnesisModelRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCompanyId(UUID companyId) {
        return jpaRepository.existsByCompanyId(companyId);
    }

    @Override
    public void delete(AnamnesisModel model) {
        jpaRepository.deleteById(model.getId());
    }

    static AnamnesisModel toDomain(AnamnesisModelJpaEntity e) {
        return AnamnesisModel.reconstitute(e.getId(), e.getCompanyId(), e.getName(), e.getDescription(),
                e.isActive(), e.isDefault(), e.getVersion(), e.getCreatedBy(), e.getCreatedAt(),
                e.getUpdatedAt());
    }

    static AnamnesisModelJpaEntity toEntity(AnamnesisModel m) {
        AnamnesisModelJpaEntity e = new AnamnesisModelJpaEntity();
        e.setId(m.getId());
        e.setCompanyId(m.getCompanyId());
        e.setName(m.getName());
        e.setDescription(m.getDescription());
        e.setActive(m.isActive());
        e.setDefault(m.isDefault());
        e.setVersion(m.getVersion());
        e.setCreatedBy(m.getCreatedBy());
        e.setCreatedAt(m.getCreatedAt());
        e.setUpdatedAt(m.getUpdatedAt());
        return e;
    }
}
