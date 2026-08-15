package com.becommerce.crm.domain.omnichannel;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ChannelTest {

    private final UUID companyId = UUID.randomUUID();

    @Test
    void create_shouldSetDefaults() {
        Channel c = Channel.create(companyId, ChannelType.WHATSAPP, ChannelProvider.FAKE,
                "Comercial", "espaco-123", "{\"wabaId\":\"x\"}", "vault:whatsapp-token");

        assertNotNull(c.getId());
        assertEquals(companyId, c.getCompanyId());
        assertEquals(ChannelType.WHATSAPP, c.getType());
        assertEquals(ChannelProvider.FAKE, c.getProvider());
        assertEquals("Comercial", c.getName());
        assertEquals(ChannelStatus.ACTIVE, c.getStatus());
        assertEquals("espaco-123", c.getExternalId());
        assertEquals("vault:whatsapp-token", c.getSecretsRef());
        assertNotNull(c.getCreatedAt());
    }

    @Test
    void update_shouldChangeFieldsAndBumpUpdatedAt() {
        Channel c = Channel.create(companyId, ChannelType.WHATSAPP, ChannelProvider.FAKE,
                "Comercial", "espaco-123", null, null);
        LocalDateTime before = c.getUpdatedAt();

        c.update("Vendas", ChannelStatus.INACTIVE, "espaco-999", "{\"a\":1}", "vault:t2");

        assertEquals("Vendas", c.getName());
        assertEquals(ChannelStatus.INACTIVE, c.getStatus());
        assertEquals("espaco-999", c.getExternalId());
        assertEquals("vault:t2", c.getSecretsRef());
        assertEquals(true, c.getUpdatedAt().compareTo(before) >= 0);
    }

    @Test
    void reconstitute_shouldRestoreAllFields() {
        UUID id = UUID.randomUUID();
        LocalDateTime ts = LocalDateTime.of(2026, 1, 1, 10, 0);
        Channel c = Channel.reconstitute(id, companyId, ChannelType.WHATSAPP, ChannelProvider.WHATSAPP_CLOUD_API,
                "N", ChannelStatus.ERROR, "ext", "{}", "vault:t", ts, ts);

        assertEquals(id, c.getId());
        assertEquals(ChannelProvider.WHATSAPP_CLOUD_API, c.getProvider());
        assertEquals(ChannelStatus.ERROR, c.getStatus());
        assertEquals(ts, c.getCreatedAt());
    }
}