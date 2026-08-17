package com.becommerce.crm.domain.notification.exception;

/**
 * Validação de domínio do módulo de Notificações.
 */
public class NotificationValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NotificationValidationException(String message) {
        super(message);
    }
}
