package com.becommerce.crm.application.catalog.dto;

import com.becommerce.crm.domain.catalog.CatalogItem;
import com.becommerce.crm.domain.catalog.CatalogItemType;

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
