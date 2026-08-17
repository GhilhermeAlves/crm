package com.becommerce.crm.application.notification.port.input;

import com.becommerce.crm.application.notification.dto.CreateNotificationRequest;
import com.becommerce.crm.application.notification.dto.NotificationResponse;

import java.util.List;
import java.util.UUID;

public interface NotificationUseCase {

    /**
     * Cria uma notificação para um usuário da empresa, marcando-a como não lida.
     * O {@code userId} é o destinatário; o {@code createdBy} é quem disparou
     * (pode ser o mesmo usuário ou o sistema).
     */
    NotificationResponse create(UUID companyId, CreateNotificationRequest request, UUID createdBy);

    /** Lista as notificações do usuário na empresa ativa (mais recentes primeiro). */
    List<NotificationResponse> listMine(UUID companyId, UUID userId);

    /** Contagem de notificações não lidas do usuário na empresa ativa. */
    long countUnread(UUID companyId, UUID userId);

    /** Marca uma notificação do usuário como lida. */
    NotificationResponse markAsRead(UUID companyId, UUID notificationId, UUID userId);

    /** Marca todas as notificações do usuário na empresa como lidas. */
    void markAllRead(UUID companyId, UUID userId);
}