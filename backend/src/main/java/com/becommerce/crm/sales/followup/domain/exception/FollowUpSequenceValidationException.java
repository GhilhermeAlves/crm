package com.becommerce.crm.sales.followup.domain.exception;

/**
 * Violação de regra de domínio de {@code FollowUpSequence}. Resulta em HTTP 400.
 */
public class FollowUpSequenceValidationException extends RuntimeException {

    public FollowUpSequenceValidationException(String message) {
        super(message);
    }
}
