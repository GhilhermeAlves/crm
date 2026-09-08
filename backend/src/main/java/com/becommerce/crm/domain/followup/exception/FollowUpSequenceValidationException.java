package com.becommerce.crm.domain.followup.exception;

/**
 * Violação de regra de domínio de {@code FollowUpSequence}. Resulta em HTTP 400.
 */
public class FollowUpSequenceValidationException extends RuntimeException {

    public FollowUpSequenceValidationException(String message) {
        super(message);
    }
}
