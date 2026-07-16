package com.becommerce.crm.domain.identity.exception;

public class RoleNotFoundException extends RuntimeException {
    public RoleNotFoundException(String message) {
        super(message);
    }

    public RoleNotFoundException() {
        super("Role not found");
    }
}
