package com.becommerce.crm.infrastructure.notification.websocket;

import com.becommerce.crm.application.notification.dto.NotificationResponse;
import com.becommerce.crm.application.notification.port.output.NotificationPusher;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Publica notificações via STOMP no destino {@code /user/{userId}/queue/notifications}.
 * O broker simples entrega ao usuário usando o principal (autenticado no CONNECT
 * pelo {@link StompAuthChannelInterceptor}); a autorização por empresa é garantida
 * pelo {@code companyId} embutido no payload e verificada no frontend.
 */
@Service
public class StompNotificationPusher implements NotificationPusher {

    private static final String USER_DESTINATION = "/queue/notifications";

    private final SimpMessagingTemplate messagingTemplate;

    public StompNotificationPusher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void push(NotificationResponse notification) {
        messagingTemplate.convertAndSendToUser(
                notification.userId().toString(),
                USER_DESTINATION,
                notification,
                Map.of("companyId", notification.companyId().toString()));
    }
}