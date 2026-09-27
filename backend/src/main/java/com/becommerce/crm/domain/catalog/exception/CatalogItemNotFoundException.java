package com.becommerce.crm.domain.catalog.exception;

import java.util.UUID;

public class CatalogItemNotFoundException extends RuntimeException {
    public CatalogItemNotFoundException(UUID id) {
        super("Item de catálogo não encontrado: " + id);
    }
}
