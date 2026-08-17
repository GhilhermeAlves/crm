package com.becommerce.crm.application.notification.service;

import com.becommerce.crm.application.notification.dto.CreateNotificationRequest;
import com.becommerce.crm.application.notification.port.output.NotificationRepository;
import com.becommerce.crm.domain.notification.Notification;
import com.becommerce.crm.domain.notification.NotificationType;
import com.becommerce.crm.domain.notification.exception.NotificationNotFoundException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;

    @InjectMocks NotificationService notificationService;

    private final UUID companyId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    private Notification notification(boolean read) {
        return Notification.reconstitute(UUID.randomUUID(), companyId, userId, NotificationType.TASK,
                "Nova tarefa", "Corpo", null, read ? LocalDateTime.now() : null,
                userId, LocalDateTime.now());
    }

    @Test
    void shouldCreateNotificationUnreadWithInfoDefault() {
        var response = notificationService.create(companyId,
                new CreateNotificationRequest(userId, null, "Título", "Corpo", null), userId);

        assertNotNull(response.id());
        assertEquals(NotificationType.INFO, response.type());
        assertFalse(response.read());
        verify(notificationRepository).save(any(Notification.class));
        assertNull(TenantContext.getCompanyId(), "contexto deve ser limpo");
    }

    @Test
    void shouldListOnlyUserNotifications() {
        Notification n = notification(false);
        when(notificationRepository.findByCompanyIdAndUserId(companyId, userId)).thenReturn(List.of(n));

        assertEquals(1, notificationService.listMine(companyId, userId).size());
        verify(notificationRepository).findByCompanyIdAndUserId(companyId, userId);
        assertNull(TenantContext.getCompanyId());
    }

    @Test
    void shouldCountUnread() {
        when(notificationRepository.countUnreadByCompanyIdAndUserId(companyId, userId)).thenReturn(3L);

        assertEquals(3L, notificationService.countUnread(companyId, userId));
        assertNull(TenantContext.getCompanyId());
    }

    @Test
    void shouldMarkAsRead() {
        Notification n = notification(false);
        when(notificationRepository.findById(n.getId())).thenReturn(Optional.of(n));

        var response = notificationService.markAsRead(companyId, n.getId(), userId);

        assertTrue(response.read());
        assertNotNull(response.readAt());
        verify(notificationRepository).save(n);
    }

    @Test
    void shouldRejectMarkingReadForAnotherUser() {
        Notification n = Notification.reconstitute(UUID.randomUUID(), companyId, UUID.randomUUID(),
                NotificationType.SYSTEM, "t", null, null, null, userId, LocalDateTime.now());
        when(notificationRepository.findById(n.getId())).thenReturn(Optional.of(n));

        assertThrows(NotificationNotFoundException.class,
                () -> notificationService.markAsRead(companyId, n.getId(), userId));
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void shouldRejectMarkingReadForAnotherCompany() {
        Notification n = Notification.reconstitute(UUID.randomUUID(), UUID.randomUUID(), userId,
                NotificationType.SYSTEM, "t", null, null, null, userId, LocalDateTime.now());
        when(notificationRepository.findById(n.getId())).thenReturn(Optional.of(n));

        assertThrows(NotificationNotFoundException.class,
                () -> notificationService.markAsRead(companyId, n.getId(), userId));
    }

    @Test
    void shouldMarkAllRead() {
        notificationService.markAllRead(companyId, userId);

        verify(notificationRepository).markAllRead(companyId, userId);
        assertNull(TenantContext.getCompanyId());
    }
}