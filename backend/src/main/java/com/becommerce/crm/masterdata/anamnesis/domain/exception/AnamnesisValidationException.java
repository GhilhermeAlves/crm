package com.becommerce.crm.masterdata.anamnesis.domain.exception;

/** Payload inválido de modelo de anamnese (resposta 400). */
public class AnamnesisValidationException extends RuntimeException {

    public AnamnesisValidationException(String message) {
        super(message);
    }
}
