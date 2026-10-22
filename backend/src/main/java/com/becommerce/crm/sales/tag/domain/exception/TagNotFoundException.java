package com.becommerce.crm.sales.tag.domain.exception;

import java.util.UUID;

public class TagNotFoundException extends RuntimeException {

    public TagNotFoundException(String entity, UUID id) {
        super(entity + " não encontrado: " + id);
    }
}
