package com.becommerce.crm.domain.followup.exception;

/**
 * Violação de regra de negócio de FollowUp (Sprint 22). Resulta em HTTP 400.
 */
public class FollowUpValidationException extends RuntimeException {

    public FollowUpValidationException(String message) {
        super(message);
    }
}