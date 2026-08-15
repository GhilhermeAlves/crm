package com.becommerce.crm.domain.omnichannel;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}