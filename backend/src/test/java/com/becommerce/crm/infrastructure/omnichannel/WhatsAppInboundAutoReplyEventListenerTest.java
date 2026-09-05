package com.becommerce.crm.infrastructure.omnichannel;

import com.becommerce.crm.application.omnichannel.service.WhatsAppInboundAutoReplyProcessor;
import com.becommerce.crm.domain.workflow.event.WorkflowTriggerEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class WhatsAppInboundAutoReplyEventListenerTest {

    private final WhatsAppInboundAutoReplyProcessor processor = mock(WhatsAppInboundAutoReplyProcessor.class);
    private final WhatsAppInboundAutoReplyEventListener listener =
            new WhatsAppInboundAutoReplyEventListener(processor);

    private final UUID companyId = UUID.randomUUID();
    private final UUID contactId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();
    private final UUID messageId = UUID.randomUUID();
    private final String from = "5511999999999";
    private final String body = "Oi!";

    @Test
    void shouldIgnoreNonWhatsAppTriggers() {
        WorkflowTriggerEvent event = WorkflowTriggerEvent.contactCreated(companyId, contactId, "a@b.c", "11");

        listener.onInboundMessage(event);

        verify(processor, never()).processInbound(any(), any(), any(), any(), any());
    }

    @Test
    void shouldDelegateWhatsAppEventToProcessor() {
        WorkflowTriggerEvent event = WorkflowTriggerEvent.whatsAppMessageReceived(
                companyId, contactId, conversationId, messageId, from, body);

        listener.onInboundMessage(event);

        verify(processor).processInbound(companyId, conversationId, messageId, from, body);
    }

    @Test
    void shouldIgnoreEventWithoutContext() {
        WorkflowTriggerEvent event = WorkflowTriggerEvent.whatsAppMessageReceived(
                companyId, contactId, conversationId, messageId, from, body);
        event.context().remove("whatsapp.body");

        listener.onInboundMessage(event);

        verify(processor, never()).processInbound(any(), any(), any(), any(), any());
    }

    @Test
    void shouldSwallowProcessorFailure() {
        WorkflowTriggerEvent event = WorkflowTriggerEvent.whatsAppMessageReceived(
                companyId, contactId, conversationId, messageId, from, body);
        doThrow(new RuntimeException("boom")).when(processor)
                .processInbound(companyId, conversationId, messageId, from, body);

        listener.onInboundMessage(event);
    }
}