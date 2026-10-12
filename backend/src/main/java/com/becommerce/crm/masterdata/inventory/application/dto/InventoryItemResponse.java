package com.becommerce.crm.masterdata.inventory.application.dto;

import com.becommerce.crm.masterdata.catalog.domain.CatalogItem;

import java.util.UUID;

public record InventoryItemResponse(
        UUID id,
        String name,
        String category,
        String unit,
        int minStock,
        int quantity,
        String sku,
        boolean active
) {
    public static InventoryItemResponse from(CatalogItem i) {
        return new InventoryItemResponse(i.getId(), i.getName(), i.getCategory(), i.getUnit(),
                i.getMinStock(), i.getQuantity(), i.getSku(), i.isActive());
    }
}
