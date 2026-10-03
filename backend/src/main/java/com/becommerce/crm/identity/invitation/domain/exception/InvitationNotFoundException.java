package com.becommerce.crm.identity.invitation.domain.exception;

public class InvitationNotFoundException extends RuntimeException {

    public InvitationNotFoundException(String message) {
        super(message);
    }
}