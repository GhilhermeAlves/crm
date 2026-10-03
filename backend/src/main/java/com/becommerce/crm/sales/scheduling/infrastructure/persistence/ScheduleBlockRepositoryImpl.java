package com.becommerce.crm.sales.scheduling.infrastructure.persistence;

import com.becommerce.crm.sales.scheduling.application.port.out.ScheduleBlockRepository;
import com.becommerce.crm.sales.scheduling.domain.BlockSource;
import com.becommerce.crm.sales.scheduling.domain.ScheduleBlock;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ScheduleBlockRepositoryImpl implements ScheduleBlockRepository {

    private final ScheduleBlockJpaRepository jpa;

    public ScheduleBlockRepositoryImpl(ScheduleBlockJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public ScheduleBlock save(ScheduleBlock block) {
        return toDomain(jpa.save(toEntity(block)));
    }

    @Override
    public Optional<ScheduleBlock> findById(UUID id) {
        return jpa.findById(id).map(ScheduleBlockRepositoryImpl::toDomain);
    }

    @Override
    public List<ScheduleBlock> findByHostIdAndRange(UUID companyId, UUID hostId, Instant from, Instant to) {
        return jpa.findByHostIdAndRange(companyId, hostId, from, to)
                .stream().map(ScheduleBlockRepositoryImpl::toDomain).toList();
    }

    @Override
    public List<ScheduleBlock> findByCompanyIdAndRange(UUID companyId, Instant from, Instant to, List<UUID> hostIds) {
        return jpa.findByCompanyIdAndRange(companyId, from, to, hostIds == null || hostIds.isEmpty() ? null : hostIds)
                .stream().map(ScheduleBlockRepositoryImpl::toDomain).toList();
    }

    @Override
    public void delete(ScheduleBlock block) {
        jpa.deleteById(block.getId());
    }

    private static ScheduleBlockJpaEntity toEntity(ScheduleBlock b) {
        ScheduleBlockJpaEntity e = new ScheduleBlockJpaEntity();
        e.setId(b.getId());
        e.setCompanyId(b.getCompanyId());
        e.setHostId(b.getHostId());
        e.setStartAt(b.getStartAt());
        e.setEndAt(b.getEndAt());
        e.setReason(b.getReason());
        e.setSource(b.getSource() != null ? b.getSource().name() : null);
        e.setExternalId(b.getExternalId());
        e.setCreatedBy(b.getCreatedBy());
        e.setCreatedAt(b.getCreatedAt());
        return e;
    }

    private static ScheduleBlock toDomain(ScheduleBlockJpaEntity e) {
        return ScheduleBlock.reconstitute(e.getId(), e.getCompanyId(), e.getHostId(),
                e.getStartAt(), e.getEndAt(), e.getReason(),
                e.getSource() != null ? BlockSource.valueOf(e.getSource()) : null,
                e.getExternalId(), e.getCreatedBy(), e.getCreatedAt());
    }
}
