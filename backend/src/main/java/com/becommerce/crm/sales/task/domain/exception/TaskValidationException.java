package com.becommerce.crm.sales.task.domain.exception;

/**
 * Violação de regra de negócio de task (Sprint 12). Resulta em HTTP 400.
 */
public class TaskValidationException extends RuntimeException {

    public TaskValidationException(String message) {
        super(message);
    }
}