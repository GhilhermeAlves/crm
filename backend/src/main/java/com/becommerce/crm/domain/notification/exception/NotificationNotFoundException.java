package com.becommerce.crm.domain.notification.exception;

import java.util.UUID;

/**
 * Lançada quando uma notificação não é encontrada (ou não pertence ao chamador).
 */
public class NotificationNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NotificationNotFoundException(UUID id) {
        super("Notificação não encontrada: " + id);
    }
}
