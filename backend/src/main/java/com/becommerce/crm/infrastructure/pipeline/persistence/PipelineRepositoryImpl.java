package com.becommerce.crm.infrastructure.pipeline.persistence;

import com.becommerce.crm.application.pipeline.port.output.PipelineRepository;
import com.becommerce.crm.domain.pipeline.Pipeline;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PipelineRepositoryImpl implements PipelineRepository {

    private final PipelineJpaRepository jpaRepository;

    public PipelineRepositoryImpl(PipelineJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Pipeline save(Pipeline pipeline) {
        return toDomain(jpaRepository.save(toEntity(pipeline)));
    }

    @Override
    public Optional<Pipeline> findById(UUID id) {
        return jpaRepository.findById(id).map(PipelineRepositoryImpl::toDomain);
    }

    @Override
    public List<Pipeline> findByCompanyId(UUID companyId) {
        return jpaRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(PipelineRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public void delete(Pipeline pipeline) {
        jpaRepository.deleteById(pipeline.getId());
    }

    private static PipelineJpaEntity toEntity(Pipeline p) {
        PipelineJpaEntity e = new PipelineJpaEntity();
        e.setId(p.getId());
        e.setCompanyId(p.getCompanyId());
        e.setName(p.getName());
        e.setDescription(p.getDescription());
        e.setActive(p.isActive());
        e.setCreatedAt(p.getCreatedAt());
        e.setUpdatedAt(p.getUpdatedAt());
        return e;
    }

    private static Pipeline toDomain(PipelineJpaEntity e) {
        return Pipeline.reconstitute(e.getId(), e.getCompanyId(), e.getName(), e.getDescription(),
                e.isActive(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
