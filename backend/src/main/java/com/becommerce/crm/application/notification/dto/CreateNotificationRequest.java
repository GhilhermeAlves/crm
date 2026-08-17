package com.becommerce.crm.application.notification.dto;

import com.becommerce.crm.domain.notification.NotificationType;

import java.util.UUID;

/**
 * Requisição de criação de notificação. Normalmente usada internamente (serviços
 * criam notificações em resposta a eventos), mas exposta para facilitar testes e
 * integração.
 */
public record CreateNotificationRequest(
        UUID userId,
        NotificationType type,
        String title,
        String body,
        String metadata
) {
}
