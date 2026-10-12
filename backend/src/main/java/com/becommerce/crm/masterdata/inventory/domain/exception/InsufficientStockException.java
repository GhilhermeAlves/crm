package com.becommerce.crm.masterdata.inventory.domain.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String itemName, int current, int requested) {
        super("Estoque insuficiente para \"" + itemName + "\" (atual: " + current
                + ", solicitado: " + requested + ").");
    }
}
