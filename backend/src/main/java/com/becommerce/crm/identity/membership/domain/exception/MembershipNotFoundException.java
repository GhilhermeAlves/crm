package com.becommerce.crm.identity.membership.domain.exception;

public class MembershipNotFoundException extends RuntimeException {

    public MembershipNotFoundException(String message) {
        super(message);
    }
}
