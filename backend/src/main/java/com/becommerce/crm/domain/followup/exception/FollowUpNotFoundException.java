package com.becommerce.crm.domain.followup.exception;

/**
 * FollowUp não encontrado (ou de outra empresa). Resulta em HTTP 404.
 */
public class FollowUpNotFoundException extends RuntimeException {

    public FollowUpNotFoundException(java.util.UUID id) {
        super("Follow-up não encontrado: " + id);
    }
}