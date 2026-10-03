package com.becommerce.crm.sales.pipeline.domain.exception;

import java.util.UUID;

public class OpportunityNotFoundException extends RuntimeException {

    public OpportunityNotFoundException(UUID id) {
        super("Oportunidade não encontrada: " + id);
    }
}
