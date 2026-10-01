package com.becommerce.crm.masterdata.catalog.domain.exception;

import java.util.UUID;

public class CatalogItemNotFoundException extends RuntimeException {
    public CatalogItemNotFoundException(UUID id) {
        super("Item de catálogo não encontrado: " + id);
    }
}
