package com.becommerce.crm.clinical.trichology.domain.exception;

/** Dado inválido enviado ao módulo de tricologia (400). */
public class TrichologyValidationException extends RuntimeException {
    public TrichologyValidationException(String message) {
        super(message);
    }
}
