package com.becommerce.crm.infrastructure.pipeline.persistence;

import com.becommerce.crm.application.pipeline.port.output.StageRepository;
import com.becommerce.crm.domain.pipeline.Stage;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class StageRepositoryImpl implements StageRepository {

    private final StageJpaRepository jpaRepository;

    public StageRepositoryImpl(StageJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Stage save(Stage stage) {
        return toDomain(jpaRepository.save(toEntity(stage)));
    }

    @Override
    public Optional<Stage> findById(UUID id) {
        return jpaRepository.findById(id).map(StageRepositoryImpl::toDomain);
    }

    @Override
    public List<Stage> findByPipelineIdOrdered(UUID pipelineId) {
        return jpaRepository.findByPipelineIdOrderByOrderNumAsc(pipelineId).stream()
                .map(StageRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public List<Stage> findByCompanyId(UUID companyId) {
        return jpaRepository.findByCompanyId(companyId).stream()
                .map(StageRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public int countByPipelineId(UUID pipelineId) {
        return (int) jpaRepository.countByPipelineId(pipelineId);
    }

    @Override
    public void delete(Stage stage) {
        jpaRepository.deleteById(stage.getId());
    }

    private static StageJpaEntity toEntity(Stage s) {
        StageJpaEntity e = new StageJpaEntity();
        e.setId(s.getId());
        e.setPipelineId(s.getPipelineId());
        e.setCompanyId(s.getCompanyId());
        e.setName(s.getName());
        e.setColor(s.getColor());
        e.setOrderNum(s.getOrderNum());
        e.setProbability(s.getProbability());
        e.setCreatedAt(s.getCreatedAt());
        e.setUpdatedAt(s.getUpdatedAt());
        return e;
    }

    private static Stage toDomain(StageJpaEntity e) {
        return Stage.reconstitute(e.getId(), e.getPipelineId(), e.getCompanyId(), e.getName(),
                e.getColor(), e.getOrderNum(), e.getProbability(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
