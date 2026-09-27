package com.becommerce.crm.domain.catalog.exception;

public class CatalogSkuConflictException extends RuntimeException {
    public CatalogSkuConflictException(String sku) {
        super("Já existe um item com o SKU " + sku + " nesta empresa.");
    }
}
