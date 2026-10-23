package com.becommerce.crm.clinical.evolution.domain.exception;

import java.util.UUID;

public class PatientEvolutionNotFoundException extends RuntimeException {

    public PatientEvolutionNotFoundException(UUID id) {
        super("Evolução não encontrada: " + id);
    }
}
