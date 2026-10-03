package com.becommerce.crm.masterdata.catalog.application.dto;

import com.becommerce.crm.masterdata.catalog.domain.CatalogItem;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItemType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CatalogItemResponse(
        UUID id,
        UUID companyId,
        CatalogItemType type,
        String name,
        String description,
        String category,
        BigDecimal price,
        String currency,
        String sku,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CatalogItemResponse from(CatalogItem i) {
        return new CatalogItemResponse(i.getId(), i.getCompanyId(), i.getType(), i.getName(),
                i.getDescription(), i.getCategory(), i.getPrice(), i.getCurrency(), i.getSku(),
                i.isActive(), i.getCreatedAt(), i.getUpdatedAt());
    }
}
