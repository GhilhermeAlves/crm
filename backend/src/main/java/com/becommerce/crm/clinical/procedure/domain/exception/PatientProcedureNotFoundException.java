package com.becommerce.crm.clinical.procedure.domain.exception;

import java.util.UUID;

public class PatientProcedureNotFoundException extends RuntimeException {

    public PatientProcedureNotFoundException(UUID id) {
        super("Procedimento não encontrado: " + id);
    }
}
