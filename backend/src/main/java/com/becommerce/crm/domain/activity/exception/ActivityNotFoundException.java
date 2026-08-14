package com.becommerce.crm.domain.activity.exception;

/**
 * Activity não encontrada (ou de outra empresa). Resulta em HTTP 404.
 */
public class ActivityNotFoundException extends RuntimeException {

    public ActivityNotFoundException(java.util.UUID id) {
        super("Atividade não encontrada: " + id);
    }
}