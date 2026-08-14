package com.becommerce.crm.domain.activity.exception;

/**
 * Violação de regra de negócio de activity (Sprint 12). Resulta em HTTP 400.
 */
public class ActivityValidationException extends RuntimeException {

    public ActivityValidationException(String message) {
        super(message);
    }
}