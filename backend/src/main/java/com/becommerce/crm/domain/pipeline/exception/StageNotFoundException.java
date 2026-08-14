package com.becommerce.crm.domain.pipeline.exception;

import java.util.UUID;

public class StageNotFoundException extends RuntimeException {

    public StageNotFoundException(UUID id) {
        super("Estágio não encontrado: " + id);
    }
}
