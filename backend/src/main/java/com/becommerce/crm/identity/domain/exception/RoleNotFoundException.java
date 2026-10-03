package com.becommerce.crm.identity.domain.exception;

public class RoleNotFoundException extends RuntimeException {
    public RoleNotFoundException(String message) {
        super(message);
    }

    public RoleNotFoundException() {
        super("Role not found");
    }
}
