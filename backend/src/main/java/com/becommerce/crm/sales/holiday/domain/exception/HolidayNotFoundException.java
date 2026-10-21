package com.becommerce.crm.sales.holiday.domain.exception;

import java.util.UUID;

public class HolidayNotFoundException extends RuntimeException {

    public HolidayNotFoundException(String entity, UUID id) {
        super(entity + " não encontrado: " + id);
    }
}
