package com.becommerce.crm.masterdata.catalog.application.dto;

import com.becommerce.crm.masterdata.catalog.domain.CatalogItem;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogLookupResponse(UUID id, String name, BigDecimal price, String currency) {

    public static CatalogLookupResponse from(CatalogItem item) {
        return new CatalogLookupResponse(item.getId(), item.getName(), item.getPrice(), item.getCurrency());
    }
}
