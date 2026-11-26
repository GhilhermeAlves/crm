package com.becommerce.crm.clinical.trichology.domain.exception;

/** Operação não permitida no estado atual do exame, ex.: editar relatório publicado (409). */
public class TrichologyStateException extends RuntimeException {
    public TrichologyStateException(String message) {
        super(message);
    }
}
