package com.becommerce.crm.masterdata.inventory.domain.exception;

import java.util.UUID;

public class InventoryItemNotFoundException extends RuntimeException {
    public InventoryItemNotFoundException(UUID id) {
        super("Item de estoque não encontrado ou não é de estoque: " + id);
    }
}
