package com.becommerce.crm.communication.telegram.application;

import com.becommerce.crm.communication.notification.application.dto.CreateNotificationRequest;
import com.becommerce.crm.communication.notification.application.port.input.NotificationUseCase;
import com.becommerce.crm.communication.notification.domain.NotificationType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramActivityNotifierTest {

    static final UUID CO = UUID.randomUUID(), USER = UUID.randomUUID(), CONV = UUID.randomUUID(),
            ACTION = UUID.randomUUID();

    @Mock NotificationUseCase notifications;
    TelegramActivityNotifier notifier;

    @BeforeEach
    void setUp() {
        notifier = new TelegramActivityNotifier(notifications, new ObjectMapper());
    }

    private CreateNotificationRequest captured() {
        ArgumentCaptor<CreateNotificationRequest> req = ArgumentCaptor.forClass(CreateNotificationRequest.class);
        verify(notifications).create(eq(CO), req.capture(), eq(USER));
        return req.getValue();
    }

    @Test
    void messageReceivedGoesToTheLinkedUserWithReplyAndPendingCount() {
        notifier.messageReceived(CO, USER, "marca a\nMaria", false, "Confirma?", 2, CONV);
        CreateNotificationRequest r = captured();
        assertEquals(USER, r.userId());
        assertEquals(NotificationType.MESSAGE, r.type());
        assertEquals("📩 Telegram: marca a Maria", r.title());
        assertTrue(r.body().startsWith("Leo: Confirma?"));
        assertTrue(r.body().contains("2 ações aguardando confirmação"));
        assertTrue(r.metadata().contains("\"channel\":\"telegram\""));
        assertTrue(r.metadata().contains(CONV.toString()));
    }

    @Test
    void audioMessageUsesMicrophoneTitle() {
        notifier.messageReceived(CO, USER, "oi", true, "Olá", 0, null);
        CreateNotificationRequest r = captured();
        assertEquals("🎙️ Telegram: oi", r.title());
        assertEquals("Leo: Olá", r.body());
    }

    @Test
    void longTextIsTruncatedToColumnLimits() {
        notifier.messageReceived(CO, USER, "a".repeat(500), false, "b".repeat(5000), 0, CONV);
        CreateNotificationRequest r = captured();
        assertEquals(TelegramActivityNotifier.TITLE_MAX, r.title().length());
        assertEquals(TelegramActivityNotifier.BODY_MAX, r.body().length());
    }

    @Test
    void executedActionTitleAndNoBody() {
        notifier.actionResult(CO, USER, ACTION, "EXECUTED", "Cancelar consulta de Maria", null);
        CreateNotificationRequest r = captured();
        assertEquals("✅ Leo (Telegram) fez: Cancelar consulta de Maria", r.title());
        assertNull(r.body());
        assertTrue(r.metadata().contains(ACTION.toString()));
    }

    @Test
    void failedActionCarriesErrorMessage() {
        notifier.actionResult(CO, USER, ACTION, "FAILED", "Lançar receita", "Sem permissão");
        CreateNotificationRequest r = captured();
        assertTrue(r.title().startsWith("⚠️ Leo (Telegram) não conseguiu"));
        assertEquals("Sem permissão", r.body());
    }

    @Test
    void notificationFailureNeverPropagates() {
        when(notifications.create(any(), any(), any())).thenThrow(new RuntimeException("db"));
        notifier.actionResult(CO, USER, ACTION, "CANCELLED", "x", null);
    }
}
