package com.becommerce.crm.application.notification.port.output;

import com.becommerce.crm.domain.notification.Notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {

    Notification save(Notification notification);

    Optional<Notification> findById(UUID id);

    /** Notificações de um usuário numa empresa, mais recentes primeiro. */
    List<Notification> findByCompanyIdAndUserId(UUID companyId, UUID userId);

    /** Contagem de notificações não lidas de um usuário numa empresa. */
    long countUnreadByCompanyIdAndUserId(UUID companyId, UUID userId);

    /** Marca todas as notificações não lidas de um usuário numa empresa como lidas. */
    void markAllRead(UUID companyId, UUID userId);
}