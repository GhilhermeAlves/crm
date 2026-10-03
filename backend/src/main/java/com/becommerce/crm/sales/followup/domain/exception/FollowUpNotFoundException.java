package com.becommerce.crm.sales.followup.domain.exception;

/**
 * FollowUp não encontrado (ou de outra empresa). Resulta em HTTP 404.
 */
public class FollowUpNotFoundException extends RuntimeException {

    public FollowUpNotFoundException(java.util.UUID id) {
        super("Follow-up não encontrado: " + id);
    }
}