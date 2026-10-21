package com.becommerce.crm.sales.recall.domain.exception;

import java.util.UUID;

public class RecallNotFoundException extends RuntimeException {

    public RecallNotFoundException(String entity, UUID id) {
        super(entity + " não encontrado: " + id);
    }
}
