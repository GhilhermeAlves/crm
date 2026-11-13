package com.becommerce.crm.communication.omnichannel.application.service;

import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelConversationRepository;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelIgnoredContactRepository;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelInboxNotifier;
import com.becommerce.crm.communication.omnichannel.domain.Conversation;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutomatedSenderGuardTest {

    private final OmnichannelIgnoredContactRepository repository = mock(OmnichannelIgnoredContactRepository.class);
    private final OmnichannelInboxNotifier notifier = mock(OmnichannelInboxNotifier.class);
    private final OmnichannelConversationRepository conversations = mock(OmnichannelConversationRepository.class);
    private final AutomatedSenderGuard guard = new AutomatedSenderGuard(repository, notifier, conversations, 168);
    private final UUID companyId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void shouldAddToIgnoredListAndNotifyTeam() {
        when(repository.existsByCompanyAndPhone(companyId, "5534999990000")).thenReturn(false);

        assertTrue(guard.ignore(companyId, conversationId, "+55 (34) 99999-0000", "menu"));

        verify(repository).save(argThat(c -> c.companyId().equals(companyId)
                && c.phone().equals("5534999990000") && AutomatedSenderGuard.LABEL.equals(c.label())));
        verify(notifier).notifyAutomatedSenderIgnored(companyId, conversationId, "5534999990000", "menu");
        assertEquals(companyId, TenantContext.getCompanyId());
    }

    @Test
    void shouldNotDuplicateAlreadyIgnoredNumber() {
        when(repository.existsByCompanyAndPhone(companyId, "5534999990000")).thenReturn(true);

        assertTrue(guard.ignore(companyId, conversationId, "5534999990000", "menu"));

        verify(repository, never()).save(any());
        verify(notifier, never()).notifyAutomatedSenderIgnored(any(), any(), any(), any());
    }

    @Test
    void shouldKeepIgnoringWhenNotificationFails() {
        doThrow(new RuntimeException("falhou")).when(notifier)
                .notifyAutomatedSenderIgnored(any(), any(), any(), any());

        assertTrue(guard.ignore(companyId, conversationId, "5534999990000", "menu"));

        verify(repository).save(any());
    }

    @Test
    void handToTeamShouldPauseConversationAndNotifyWithoutIgnoringNumber() {
        Conversation conversation = Conversation.create(companyId, UUID.randomUUID(), null, "5534999990000");
        when(conversations.findById(conversationId)).thenReturn(Optional.of(conversation));

        assertTrue(guard.handToTeam(companyId, conversationId, "+55 (34) 99999-0000", "agente: assunto pessoal"));

        assertTrue(conversation.isInHumanMode());
        assertTrue(conversation.getHumanUntil().isAfter(LocalDateTime.now().plusDays(6)));
        verify(conversations).save(conversation);
        verify(notifier).notifyHandedToTeam(companyId, conversationId, "5534999990000", "agente: assunto pessoal", 7);
        verify(repository, never()).save(any());
        assertEquals(companyId, TenantContext.getCompanyId());
    }

    @Test
    void handToTeamShouldNotTouchConversationOfAnotherCompany() {
        Conversation other = Conversation.create(UUID.randomUUID(), UUID.randomUUID(), null, "5534999990000");
        when(conversations.findById(conversationId)).thenReturn(Optional.of(other));

        assertFalse(guard.handToTeam(companyId, conversationId, "5534999990000", "motivo"));

        verify(conversations, never()).save(any());
        verify(notifier, never()).notifyHandedToTeam(any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void shouldRejectInvalidPhone() {
        assertFalse(guard.ignore(companyId, conversationId, "123", "menu"));
        verify(repository, never()).save(any());
        verify(notifier, never()).notifyAutomatedSenderIgnored(eq(companyId), any(), any(), any());
    }
}
