package com.becommerce.crm.sales.chair.domain.exception;

import java.util.UUID;

public class ChairNotFoundException extends RuntimeException {

    public ChairNotFoundException(String entity, UUID id) {
        super(entity + " não encontrado: " + id);
    }
}
