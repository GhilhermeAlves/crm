package com.becommerce.crm.sales.lead.domain.exception;

import java.util.UUID;

public class LeadNotFoundException extends RuntimeException {

    public LeadNotFoundException(UUID id) {
        super("Lead não encontrado: " + id);
    }
}