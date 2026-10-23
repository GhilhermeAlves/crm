package com.becommerce.crm.clinical.anamnesis.domain.exception;

import java.util.UUID;

public class PatientAnamnesisNotFoundException extends RuntimeException {

    public PatientAnamnesisNotFoundException(UUID id) {
        super("Anamnese não encontrada: " + id);
    }
}
