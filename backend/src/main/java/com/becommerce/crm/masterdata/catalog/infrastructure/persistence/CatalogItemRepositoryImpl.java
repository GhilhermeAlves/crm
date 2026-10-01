package com.becommerce.crm.masterdata.catalog.infrastructure.persistence;

import com.becommerce.crm.masterdata.catalog.application.port.output.CatalogItemRepository;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItem;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItemType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CatalogItemRepositoryImpl implements CatalogItemRepository {

    private final CatalogItemJpaRepository jpaRepository;

    public CatalogItemRepositoryImpl(CatalogItemJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public CatalogItem save(CatalogItem item) {
        return toDomain(jpaRepository.save(toEntity(item)));
    }

    @Override
    public Optional<CatalogItem> findById(UUID id) {
        return jpaRepository.findById(id).map(CatalogItemRepositoryImpl::toDomain);
    }

    @Override
    public boolean existsBySku(UUID companyId, String sku, UUID excludeId) {
        return jpaRepository.existsBySku(companyId, sku, excludeId);
    }

    @Override
    public PageResult search(UUID companyId, String query, CatalogItemType type, Boolean active,
                             int page, int pageSize) {
        String pattern = query == null ? null : "%" + escapeLike(query.toLowerCase(Locale.ROOT)) + "%";
        Page<CatalogItemJpaEntity> result = jpaRepository.search(companyId,
                type == null ? null : type.name(), active, pattern,
                PageRequest.of(page, pageSize, Sort.by(Sort.Direction.ASC, "name")));
        return new PageResult(result.getContent().stream().map(CatalogItemRepositoryImpl::toDomain).toList(),
                result.getTotalElements());
    }

    /** '%' e '_' digitados pelo usuário são literais, não curingas. */
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    static CatalogItem toDomain(CatalogItemJpaEntity e) {
        return CatalogItem.reconstitute(e.getId(), e.getCompanyId(), CatalogItemType.valueOf(e.getItemType()),
                e.getName(), e.getDescription(), e.getCategory(), e.getPrice(), e.getCurrency(), e.getSku(),
                e.isActive(), e.getCreatedAt(), e.getUpdatedAt());
    }

    static CatalogItemJpaEntity toEntity(CatalogItem i) {
        CatalogItemJpaEntity e = new CatalogItemJpaEntity();
        e.setId(i.getId());
        e.setCompanyId(i.getCompanyId());
        e.setItemType(i.getType().name());
        e.setName(i.getName());
        e.setDescription(i.getDescription());
        e.setCategory(i.getCategory());
        e.setPrice(i.getPrice());
        e.setCurrency(i.getCurrency());
        e.setSku(i.getSku());
        e.setActive(i.isActive());
        e.setCreatedAt(i.getCreatedAt());
        e.setUpdatedAt(i.getUpdatedAt());
        return e;
    }
}
