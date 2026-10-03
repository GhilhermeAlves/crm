package com.becommerce.crm.identity.domain.exception;

public class PermissionNotFoundException extends RuntimeException {
    public PermissionNotFoundException(String message) {
        super(message);
    }

    public PermissionNotFoundException() {
        super("Permission not found");
    }
}
