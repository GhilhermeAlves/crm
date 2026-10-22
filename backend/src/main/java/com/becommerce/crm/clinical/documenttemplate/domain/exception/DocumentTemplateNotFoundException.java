package com.becommerce.crm.clinical.documenttemplate.domain.exception;

import java.util.UUID;

public class DocumentTemplateNotFoundException extends RuntimeException {

    public DocumentTemplateNotFoundException(UUID id) {
        super("Modelo de documento não encontrado: " + id);
    }
}
