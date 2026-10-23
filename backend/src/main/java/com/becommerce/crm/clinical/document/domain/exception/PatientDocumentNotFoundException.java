package com.becommerce.crm.clinical.document.domain.exception;

import java.util.UUID;

public class PatientDocumentNotFoundException extends RuntimeException {

    public PatientDocumentNotFoundException(UUID id) {
        super("Documento não encontrado: " + id);
    }
}
