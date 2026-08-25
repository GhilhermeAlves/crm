package com.becommerce.crm.domain.template.exception;

import java.util.UUID;

public class TemplateNotFoundException extends RuntimeException {

    public TemplateNotFoundException(UUID id) {
        super("Template não encontrado: " + id);
    }
}
