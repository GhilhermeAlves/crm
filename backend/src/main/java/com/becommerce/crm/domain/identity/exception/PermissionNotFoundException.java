package com.becommerce.crm.domain.identity.exception;

public class PermissionNotFoundException extends RuntimeException {
    public PermissionNotFoundException(String message) {
        super(message);
    }

    public PermissionNotFoundException() {
        super("Permission not found");
    }
}
