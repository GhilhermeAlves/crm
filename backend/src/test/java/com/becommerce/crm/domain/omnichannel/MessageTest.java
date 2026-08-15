package com.becommerce.crm.domain.omnichannel;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class MessageTest {

    private final UUID companyId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();
    private final UUID channelId = UUID.randomUUID();

    @Test
    void createInbound_shouldSetDirectionAndSENTStatus() {
        Message m = Message.createInbound(companyId, conversationId, channelId,
                "+5511999998888", "espaco-a", "Oi, quero uma proposta", "wamid-1");

        assertEquals(MessageDirection.INBOUND, m.getDirection());
        assertEquals(MessageStatus.SENT, m.getStatus());
        assertEquals("wamid-1", m.getExternalMessageId());
        assertEquals("+5511999998888", m.getSenderPhone());
        assertEquals("Oi, quero uma proposta", m.getBody());
        assertNotNull(m.getClientMessageId());
        assertNotNull(m.getReceivedAt());
    }

    @Test
    void createOutbound_shouldBePENDINGWithoutExternalId() {
        UUID clientId = UUID.randomUUID();
        Message m = Message.createOutbound(companyId, conversationId, channelId,
                "espaco-a", "+5511999998888", "Bom dia!", clientId);

        assertEquals(MessageDirection.OUTBOUND, m.getDirection());
        assertEquals(MessageStatus.PENDING, m.getStatus());
        assertNull(m.getExternalMessageId());
        assertEquals(clientId, m.getClientMessageId());
        assertNull(m.getSentAt());
    }

    @Test
    void markSent_shouldSetExternalIdAndSENT() {
        Message m = Message.createOutbound(companyId, conversationId, channelId,
                "espaco-a", "+5511999998888", "ok", UUID.randomUUID());
        m.markSent("wamid-999");

        assertEquals(MessageStatus.SENT, m.getStatus());
        assertEquals("wamid-999", m.getExternalMessageId());
        assertNotNull(m.getSentAt());
    }

    @Test
    void markStatus_deliveredAndRead() {
        Message m = Message.createOutbound(companyId, conversationId, channelId,
                "espaco-a", "+5511999998888", "ok", UUID.randomUUID());
        m.markStatus(MessageStatus.DELIVERED, null);
        assertEquals(MessageStatus.DELIVERED, m.getStatus());

        m.markStatus(MessageStatus.READ, null);
        assertEquals(MessageStatus.READ, m.getStatus());
    }

    @Test
    void markStatus_failed_shouldRecordError() {
        Message m = Message.createOutbound(companyId, conversationId, channelId,
                "espaco-a", "+5511999998888", "ok", UUID.randomUUID());
        m.markStatus(MessageStatus.FAILED, "131026: número inválido");

        assertEquals(MessageStatus.FAILED, m.getStatus());
        assertEquals("131026: número inválido", m.getProviderError());
    }
}