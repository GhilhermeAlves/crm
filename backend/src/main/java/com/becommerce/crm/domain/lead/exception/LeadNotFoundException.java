package com.becommerce.crm.domain.lead.exception;

import java.util.UUID;

public class LeadNotFoundException extends RuntimeException {

    public LeadNotFoundException(UUID id) {
        super("Lead não encontrado: " + id);
    }
}