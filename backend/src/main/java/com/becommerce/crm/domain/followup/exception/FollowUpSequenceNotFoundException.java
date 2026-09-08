package com.becommerce.crm.domain.followup.exception;

/**
 * FollowUpSequence não encontrada (ou de outra empresa). Resulta em HTTP 404.
 */
public class FollowUpSequenceNotFoundException extends RuntimeException {

    public FollowUpSequenceNotFoundException(java.util.UUID id) {
        super("Sequência de follow-up não encontrada: " + id);
    }
}
