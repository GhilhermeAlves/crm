package com.becommerce.crm.sales.pipeline.domain.exception;

import java.util.UUID;

public class StageNotFoundException extends RuntimeException {

    public StageNotFoundException(UUID id) {
        super("Estágio não encontrado: " + id);
    }
}
