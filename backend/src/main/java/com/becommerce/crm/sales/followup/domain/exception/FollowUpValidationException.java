package com.becommerce.crm.sales.followup.domain.exception;

/**
 * Violação de regra de negócio de FollowUp (Sprint 22). Resulta em HTTP 400.
 */
public class FollowUpValidationException extends RuntimeException {

    public FollowUpValidationException(String message) {
        super(message);
    }
}