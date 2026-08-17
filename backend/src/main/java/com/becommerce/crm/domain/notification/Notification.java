package com.becommerce.crm.domain.notification;

import com.becommerce.crm.domain.notification.exception.NotificationValidationException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notification (módulo de Notificações) — alerta in-app direcionado a um usuário
 * (user_id) de uma empresa (company_id, RLS FORCE). Cada notificação é pessoal e
 * pode ser marcada como lida.
 *
 * <p>Corresponde à tabela {@code notifications} (V047).</p>
 */
public class Notification {

    private final UUID id;
    private final UUID companyId;
    private final UUID userId;
    private final NotificationType type;
    private String title;
    private String body;
    private String metadata;
    private LocalDateTime readAt;
    private final UUID createdBy;
    private final LocalDateTime createdAt;

    private Notification(UUID id, UUID companyId, UUID userId, NotificationType type, String title,
                         String body, String metadata, LocalDateTime readAt, UUID createdBy,
                         LocalDateTime createdAt) {
        this.id = id;
        this.companyId = companyId;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.metadata = metadata;
        this.readAt = readAt;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public static Notification create(UUID companyId, UUID userId, NotificationType type, String title,
                                      String body, String metadata, UUID createdBy) {
        if (userId == null) {
            throw new NotificationValidationException("O destinatário da notificação é obrigatório.");
        }
        if (title == null || title.isBlank()) {
            throw new NotificationValidationException("O título da notificação é obrigatório.");
        }
        String normalized = title.trim();
        if (normalized.length() > 200) {
            throw new NotificationValidationException("O título deve ter no máximo 200 caracteres.");
        }
        NotificationType t = type != null ? type : NotificationType.INFO;
        return new Notification(UUID.randomUUID(), companyId, userId, t, normalized, body, metadata,
                null, createdBy, LocalDateTime.now());
    }

    public static Notification reconstitute(UUID id, UUID companyId, UUID userId, NotificationType type,
                                            String title, String body, String metadata, LocalDateTime readAt,
                                            UUID createdBy, LocalDateTime createdAt) {
        return new Notification(id, companyId, userId, type, title, body, metadata, readAt, createdBy, createdAt);
    }

    public void markAsRead() {
        if (readAt == null) {
            this.readAt = LocalDateTime.now();
        }
    }

    public boolean isRead() {
        return readAt != null;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getUserId() { return userId; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getMetadata() { return metadata; }
    public LocalDateTime getReadAt() { return readAt; }
    public UUID getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
