package com.becommerce.crm.domain.pipeline.exception;

import java.util.UUID;

public class OpportunityNotFoundException extends RuntimeException {

    public OpportunityNotFoundException(UUID id) {
        super("Oportunidade não encontrada: " + id);
    }
}
