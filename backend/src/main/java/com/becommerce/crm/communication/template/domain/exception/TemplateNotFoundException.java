package com.becommerce.crm.communication.template.domain.exception;

import java.util.UUID;

public class TemplateNotFoundException extends RuntimeException {

    public TemplateNotFoundException(UUID id) {
        super("Template não encontrado: " + id);
    }
}
