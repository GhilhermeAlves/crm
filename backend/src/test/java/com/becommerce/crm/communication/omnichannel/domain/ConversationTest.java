package com.becommerce.crm.communication.omnichannel.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationTest {

    private final UUID companyId = UUID.randomUUID();
    private final UUID channelId = UUID.randomUUID();
    private final UUID contactId = UUID.randomUUID();

    @Test
    void create_shouldOpenWithZeroUnread() {
        Conversation c = Conversation.create(companyId, channelId, contactId, "+5511999998888");

        assertEquals(companyId, c.getCompanyId());
        assertEquals(channelId, c.getChannelId());
        assertEquals(contactId, c.getContactId());
        assertEquals("+5511999998888", c.getExternalPhone());
        assertEquals(ConversationStatus.OPEN, c.getStatus());
        assertEquals(0, c.getUnreadCount());
        assertNull(c.getLastMessageAt());
    }

    @Test
    void pauseAutomationUntil_ficaHumanoAteOPrazoEDepoisVoltaSozinho() {
        Conversation c = Conversation.create(companyId, channelId, contactId, "5511999998888");

        c.pauseAutomationUntil(LocalDateTime.now().plusHours(12));
        assertTrue(c.isInHumanMode());
        assertEquals(ConversationMode.HUMAN, c.getMode());

        c.restoreHumanUntil(LocalDateTime.now().minusMinutes(1));
        assertFalse(c.isInHumanMode());
        assertEquals(ConversationMode.AUTOMATIC, c.getMode());
        assertNull(c.getHumanUntil());
    }

    @Test
    void takeoverManual_naoTemPrazo_eReleaseLimpaPausa() {
        Conversation c = Conversation.create(companyId, channelId, contactId, "5511999998888");
        c.pauseAutomationUntil(LocalDateTime.now().plusHours(12));

        c.takeover();
        assertTrue(c.isInHumanMode());
        assertNull(c.getHumanUntil());

        c.pauseAutomationUntil(LocalDateTime.now().plusHours(12));
        c.releaseAutomation();
        assertFalse(c.isInHumanMode());
        assertNull(c.getHumanUntil());
    }

    @Test
    void ignoredContact_normalizaTelefone() {
        assertEquals("5534999998888", IgnoredContact.normalizePhone("(34) 99999-8888"));
        assertEquals("5534999998888", IgnoredContact.normalizePhone("+55 34 99999-8888"));
        assertEquals("", IgnoredContact.normalizePhone(null));
    }

    @Test
    void ignoredContact_variantesComESemNonoDigito() {
        assertEquals(java.util.List.of("5534991546422", "553491546422"),
                IgnoredContact.phoneVariants("5534991546422"));
        assertEquals(java.util.List.of("553491546422", "5534991546422"),
                IgnoredContact.phoneVariants("553491546422"));
        // Fixo (começa com 3) não ganha o 9.
        assertEquals(java.util.List.of("553432345678"), IgnoredContact.phoneVariants("553432345678"));
    }

    @Test
    void touch_inbound_shouldIncrementUnreadAndSetLastMessageAt() {
        Conversation c = Conversation.create(companyId, channelId, contactId, "+5511999998888");
        LocalDateTime at = LocalDateTime.now();
        c.touch(at, true);

        assertEquals(at, c.getLastMessageAt());
        assertEquals(1, c.getUnreadCount());
    }

    @Test
    void touch_outbound_shouldNotIncrementUnread() {
        Conversation c = Conversation.create(companyId, channelId, contactId, "+5511999998888");
        c.touch(LocalDateTime.now(), false);
        assertEquals(0, c.getUnreadCount());
    }

    @Test
    void markRead_shouldClearUnread() {
        Conversation c = Conversation.create(companyId, channelId, contactId, "+5511999998888");
        c.touch(LocalDateTime.now(), true);
        c.touch(LocalDateTime.now(), true);
        assertEquals(2, c.getUnreadCount());

        c.markRead();
        assertEquals(0, c.getUnreadCount());
    }

    @Test
    void closeAndReopen_shouldToggleStatus() {
        Conversation c = Conversation.create(companyId, channelId, contactId, "+5511999998888");
        c.close();
        assertEquals(ConversationStatus.CLOSED, c.getStatus());
        c.reopen();
        assertEquals(ConversationStatus.OPEN, c.getStatus());
    }

    @Test
    void assignContact_shouldLinkContact() {
        Conversation c = Conversation.create(companyId, channelId, null, "+5511999998888");
        c.assignContact(contactId);
        assertEquals(contactId, c.getContactId());
    }

    @Test
    void create_shouldStartInAutomaticMode() {
        Conversation c = Conversation.create(companyId, channelId, contactId, "+5511999998888");

        assertEquals(ConversationMode.AUTOMATIC, c.getMode());
        assertFalse(c.isInHumanMode());
    }

    @Test
    void takeover_shouldSwitchToHumanMode() {
        Conversation c = Conversation.create(companyId, channelId, contactId, "+5511999998888");

        c.takeover();

        assertEquals(ConversationMode.HUMAN, c.getMode());
        assertTrue(c.isInHumanMode());
    }

    @Test
    void takeover_whenAlreadyHuman_shouldBeIdempotent() {
        Conversation c = Conversation.create(companyId, channelId, contactId, "+5511999998888");
        c.takeover();
        c.takeover();

        assertEquals(ConversationMode.HUMAN, c.getMode());
        assertTrue(c.isInHumanMode());
    }

    @Test
    void releaseAutomation_shouldRestoreAutomaticMode() {
        Conversation c = Conversation.create(companyId, channelId, contactId, "+5511999998888");
        c.takeover();

        c.releaseAutomation();

        assertEquals(ConversationMode.AUTOMATIC, c.getMode());
        assertFalse(c.isInHumanMode());
    }

    @Test
    void releaseAutomation_whenAlreadyAutomatic_shouldBeIdempotent() {
        Conversation c = Conversation.create(companyId, channelId, contactId, "+5511999998888");
        c.releaseAutomation();
        assertEquals(ConversationMode.AUTOMATIC, c.getMode());
    }

    @Test
    void reconstitute_withMode_shouldRestoreHumanMode() {
        LocalDateTime now = LocalDateTime.now();
        Conversation c = Conversation.reconstitute(UUID.randomUUID(), companyId, channelId, contactId,
                "+5511999998888", ConversationStatus.OPEN, ConversationMode.HUMAN,
                now, 1, now, now);

        assertEquals(ConversationMode.HUMAN, c.getMode());
        assertTrue(c.isInHumanMode());
    }

    @Test
    void reconstitute_tenArgs_shouldDefaultToAutomatic() {
        LocalDateTime now = LocalDateTime.now();
        Conversation c = Conversation.reconstitute(UUID.randomUUID(), companyId, channelId, contactId,
                "+5511999998888", ConversationStatus.OPEN, now, 1, now, now);

        assertEquals(ConversationMode.AUTOMATIC, c.getMode());
    }
}