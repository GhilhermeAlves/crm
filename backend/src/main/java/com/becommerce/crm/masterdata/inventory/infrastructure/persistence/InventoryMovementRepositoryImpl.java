package com.becommerce.crm.masterdata.inventory.infrastructure.persistence;

import com.becommerce.crm.masterdata.inventory.application.port.output.StockMovementRepository;
import com.becommerce.crm.masterdata.inventory.domain.StockMovement;
import com.becommerce.crm.masterdata.inventory.domain.StockMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class InventoryMovementRepositoryImpl implements StockMovementRepository {

    private final InventoryMovementJpaRepository jpaRepository;

    public InventoryMovementRepositoryImpl(InventoryMovementJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public StockMovement save(StockMovement movement) {
        return toDomain(jpaRepository.save(toEntity(movement)));
    }

    @Override
    public StockMovementRepository.PageResult findByCatalogItem(UUID companyId, UUID catalogItemId,
                                                                int page, int pageSize) {
        Page<InventoryMovementJpaEntity> result = jpaRepository.findByCatalogItem(companyId, catalogItemId,
                PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new StockMovementRepository.PageResult(
                result.getContent().stream().map(InventoryMovementRepositoryImpl::toDomain).toList(),
                result.getTotalElements());
    }

    static StockMovement toDomain(InventoryMovementJpaEntity e) {
        return StockMovement.reconstitute(e.getId(), e.getCompanyId(), e.getCatalogItemId(),
                StockMovementType.valueOf(e.getMovementType()), e.getDelta(), e.getNote(), e.getCreatedAt());
    }

    static InventoryMovementJpaEntity toEntity(StockMovement m) {
        InventoryMovementJpaEntity e = new InventoryMovementJpaEntity();
        e.setId(m.getId());
        e.setCompanyId(m.getCompanyId());
        e.setCatalogItemId(m.getCatalogItemId());
        e.setMovementType(m.getType().name());
        e.setDelta(m.getDelta());
        e.setNote(m.getNote());
        e.setCreatedAt(m.getCreatedAt());
        return e;
    }
}
