package com.becommerce.crm.communication.telegram.application;

import com.becommerce.crm.communication.notification.application.dto.CreateNotificationRequest;
import com.becommerce.crm.communication.notification.application.port.input.NotificationUseCase;
import com.becommerce.crm.communication.notification.domain.NotificationType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Registra no sino (notificações in-app) o que o Leo fez a partir do Telegram:
 * cada mensagem recebida com o início da resposta e cada ação confirmada,
 * que falhou ou foi cancelada. A notificação vai para o próprio usuário
 * vinculado. Melhor esforço: falha aqui nunca interrompe o atendimento.
 */
@Component
public class TelegramActivityNotifier {

    private static final Logger log = LoggerFactory.getLogger(TelegramActivityNotifier.class);
    static final int TITLE_MAX = 200;
    static final int BODY_MAX = 1000;

    private final NotificationUseCase notifications;
    private final ObjectMapper mapper;

    public TelegramActivityNotifier(NotificationUseCase notifications, ObjectMapper mapper) {
        this.notifications = notifications;
        this.mapper = mapper;
    }

    /** Mensagem recebida pelo Telegram e o início da resposta do Leo. */
    public void messageReceived(UUID companyId, UUID userId, String text, boolean audio, String reply,
                                int proposedActions, UUID conversationId) {
        String title = (audio ? "🎙️ Telegram: " : "📩 Telegram: ") + oneLine(text);
        StringBuilder body = new StringBuilder("Leo: ").append(reply == null || reply.isBlank() ? "(sem resposta)" : reply);
        if (proposedActions > 0) {
            body.append("\n\n").append(proposedActions == 1
                    ? "1 ação aguardando confirmação no Telegram."
                    : proposedActions + " ações aguardando confirmação no Telegram.");
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("channel", "telegram");
        meta.put("event", "TELEGRAM_MESSAGE");
        if (conversationId != null) {
            meta.put("conversationId", conversationId.toString());
        }
        create(companyId, userId, title, body.toString(), meta);
    }

    /** Resultado de uma ação confirmada ou cancelada pelos botões. */
    public void actionResult(UUID companyId, UUID userId, UUID actionId, String status, String description,
                             String errorMessage) {
        String title;
        if ("EXECUTED".equals(status)) {
            title = "✅ Leo (Telegram) fez: " + oneLine(description);
        } else if ("CANCELLED".equals(status)) {
            title = "❌ Leo (Telegram) cancelado: " + oneLine(description);
        } else {
            title = "⚠️ Leo (Telegram) não conseguiu: " + oneLine(description);
        }
        String body = errorMessage != null && !"EXECUTED".equals(status) ? errorMessage : null;
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("channel", "telegram");
        meta.put("event", "TELEGRAM_ACTION");
        meta.put("actionId", actionId.toString());
        meta.put("status", status == null ? "" : status);
        create(companyId, userId, title, body, meta);
    }

    private void create(UUID companyId, UUID userId, String title, String body, Map<String, Object> meta) {
        try {
            notifications.create(companyId, new CreateNotificationRequest(userId, NotificationType.MESSAGE,
                    truncate(title, TITLE_MAX), body == null ? null : truncate(body, BODY_MAX),
                    mapper.writeValueAsString(meta)), userId);
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("[TELEGRAM] falha ao registrar notificação (company={})", companyId);
        }
    }

    private static String oneLine(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").strip();
    }

    static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
