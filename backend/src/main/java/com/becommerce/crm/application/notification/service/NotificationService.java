package com.becommerce.crm.application.notification.service;

import com.becommerce.crm.application.notification.dto.CreateNotificationRequest;
import com.becommerce.crm.application.notification.dto.NotificationResponse;
import com.becommerce.crm.application.notification.port.input.NotificationUseCase;
import com.becommerce.crm.application.notification.port.output.NotificationPusher;
import com.becommerce.crm.application.notification.port.output.NotificationRepository;
import com.becommerce.crm.domain.notification.Notification;
import com.becommerce.crm.domain.notification.exception.NotificationNotFoundException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Notificações in-app (módulo de Notificações). Cada operação isola a empresa
 * ativa no {@link TenantContext} (finally {@code clear()}). As notificações são
 * pessoais: listagem e marcação de leitura validam que o destinatário
 * ({@code user_id}) é o próprio usuário autenticado (defense-in-depth além da
 * RLS por tenant).
 */
@Service
public class NotificationService implements NotificationUseCase {

    private final NotificationRepository notificationRepository;
    private final NotificationPusher notificationPusher;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationPusher notificationPusher) {
        this.notificationRepository = notificationRepository;
        this.notificationPusher = notificationPusher;
    }

    @Override
    @Transactional
    public NotificationResponse create(UUID companyId, CreateNotificationRequest request, UUID createdBy) {
        try {
            TenantContext.setCompanyId(companyId);
            Notification notification = Notification.create(companyId, request.userId(), request.type(),
                    request.title(), request.body(), request.metadata(), createdBy);
            notificationRepository.save(notification);
            NotificationResponse response = toResponse(notification);
            // Push em tempo real após persistir; a falha aqui não deve abortar a
            // criação (a notificação continua no banco e pode ser listada por REST).
            try {
                notificationPusher.push(response);
            } catch (RuntimeException e) {
                // logout e segue
            }
            return response;
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> listMine(UUID companyId, UUID userId) {
        try {
            TenantContext.setCompanyId(companyId);
            return notificationRepository.findByCompanyIdAndUserId(companyId, userId).stream()
                    .map(NotificationService::toResponse).toList();
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(UUID companyId, UUID userId) {
        try {
            TenantContext.setCompanyId(companyId);
            return notificationRepository.countUnreadByCompanyIdAndUserId(companyId, userId);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID companyId, UUID notificationId, UUID userId) {
        try {
            TenantContext.setCompanyId(companyId);
            Notification notification = requireOwned(companyId, notificationId, userId);
            notification.markAsRead();
            notificationRepository.save(notification);
            return toResponse(notification);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void markAllRead(UUID companyId, UUID userId) {
        try {
            TenantContext.setCompanyId(companyId);
            notificationRepository.markAllRead(companyId, userId);
        } finally {
            TenantContext.clear();
        }
    }

    private Notification requireOwned(UUID companyId, UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        if (!notification.getCompanyId().equals(companyId)
                || !notification.getUserId().equals(userId)) {
            throw new NotificationNotFoundException(notificationId);
        }
        return notification;
    }

    private static NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getCompanyId(), n.getUserId(), n.getType(),
                n.getTitle(), n.getBody(), n.getMetadata(), n.getReadAt(), n.isRead(), n.getCreatedAt());
    }
}