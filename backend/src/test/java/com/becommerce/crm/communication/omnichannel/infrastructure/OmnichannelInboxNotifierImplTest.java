package com.becommerce.crm.communication.omnichannel.infrastructure;

import com.becommerce.crm.communication.notification.application.dto.CreateNotificationRequest;
import com.becommerce.crm.communication.notification.application.port.input.NotificationUseCase;
import com.becommerce.crm.communication.notification.domain.NotificationType;
import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.identity.domain.User;
import com.becommerce.crm.identity.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OmnichannelInboxNotifierImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final NotificationUseCase notificationUseCase = mock(NotificationUseCase.class);
    private final OmnichannelInboxNotifierImpl notifier =
            new OmnichannelInboxNotifierImpl(userRepository, notificationUseCase);

    private static User user(UserStatus status) {
        User u = mock(User.class);
        when(u.getId()).thenReturn(UUID.randomUUID());
        when(u.getStatus()).thenReturn(status);
        return u;
    }

    @Test
    void notificaApenasUsuariosAtivos_comTituloECorpo() {
        UUID companyId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        User ativo = user(UserStatus.ACTIVE);
        User inativo = user(UserStatus.INACTIVE);
        User pendente = user(UserStatus.PENDING);
        when(userRepository.findAllByCompanyId(companyId)).thenReturn(List.of(ativo, inativo, pendente));

        notifier.notifyNewInbound(companyId, conversationId, "5534991546422", "Preciso  marcar\numa consulta");

        ArgumentCaptor<CreateNotificationRequest> captor = ArgumentCaptor.forClass(CreateNotificationRequest.class);
        verify(notificationUseCase, times(1)).create(eq(companyId), captor.capture(), any());
        CreateNotificationRequest req = captor.getValue();
        assertEquals(ativo.getId(), req.userId());
        assertEquals(NotificationType.MESSAGE, req.type());
        assertEquals("Nova mensagem no WhatsApp", req.title());
        assertEquals("+55 (34) 99154-6422: Preciso marcar uma consulta", req.body());
        assertTrue(req.metadata().contains(conversationId.toString()));
    }

    @Test
    void formataTelefone() {
        assertEquals("+55 (34) 99154-6422", OmnichannelInboxNotifierImpl.formatPhone("5534991546422"));
        assertEquals("+55 (34) 9154-6422", OmnichannelInboxNotifierImpl.formatPhone("553491546422"));
        assertEquals("+14155550100", OmnichannelInboxNotifierImpl.formatPhone("14155550100"));
    }
}
