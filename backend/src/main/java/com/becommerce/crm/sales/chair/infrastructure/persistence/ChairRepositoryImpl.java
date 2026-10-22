package com.becommerce.crm.sales.chair.infrastructure.persistence;

import com.becommerce.crm.sales.chair.application.port.out.ChairRepository;
import com.becommerce.crm.sales.chair.domain.Chair;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ChairRepositoryImpl implements ChairRepository {

    private final ChairJpaRepository jpa;

    public ChairRepositoryImpl(ChairJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Chair save(Chair chair) {
        return toDomain(jpa.save(toEntity(chair)));
    }

    @Override
    public Optional<Chair> findById(UUID id) {
        return jpa.findById(id).map(ChairRepositoryImpl::toDomain);
    }

    @Override
    public List<Chair> findByCompanyId(UUID companyId) {
        return jpa.findByCompanyIdOrderBySortOrderAscNameAsc(companyId).stream()
                .map(ChairRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public Optional<Chair> findByCompanyIdAndName(UUID companyId, String name) {
        return jpa.findByCompanyIdAndName(companyId, name).map(ChairRepositoryImpl::toDomain);
    }

    @Override
    public void delete(Chair chair) {
        jpa.deleteById(chair.getId());
    }

    private static ChairJpaEntity toEntity(Chair chair) {
        ChairJpaEntity entity = new ChairJpaEntity();
        entity.setId(chair.getId());
        entity.setCompanyId(chair.getCompanyId());
        entity.setName(chair.getName());
        entity.setColor(chair.getColor());
        entity.setActive(chair.isActive());
        entity.setSortOrder(chair.getSortOrder());
        entity.setCreatedAt(chair.getCreatedAt());
        entity.setUpdatedAt(chair.getUpdatedAt());
        return entity;
    }

    private static Chair toDomain(ChairJpaEntity entity) {
        return Chair.reconstitute(entity.getId(), entity.getCompanyId(), entity.getName(), entity.getColor(),
                entity.isActive(), entity.getSortOrder(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
