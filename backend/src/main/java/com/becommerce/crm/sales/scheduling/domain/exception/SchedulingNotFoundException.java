package com.becommerce.crm.sales.scheduling.domain.exception;

import java.util.UUID;

public class SchedulingNotFoundException extends RuntimeException {

    public SchedulingNotFoundException(String entity, UUID id) {
        super(entity + " não encontrado: " + id);
    }
}
