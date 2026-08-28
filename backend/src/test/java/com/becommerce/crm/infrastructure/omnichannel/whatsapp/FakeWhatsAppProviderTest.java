package com.becommerce.crm.infrastructure.omnichannel.whatsapp;

import com.becommerce.crm.application.omnichannel.port.output.WhatsAppProvider;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FakeWhatsAppProviderTest {

    private final FakeWhatsAppProvider provider = new FakeWhatsAppProvider();

    @Test
    void providerName_shouldBeFake() {
        assertTrue("FAKE".equals(provider.providerName()));
    }

    @Test
    void send_shouldReturnFakeWamidFormat() {
        WhatsAppProvider.SendResult result = provider.send(new WhatsAppProvider.SendRequest(
                UUID.randomUUID(), UUID.randomUUID(), "1234567890", "5511999998888", "oi", null));

        assertNotNull(result);
        assertNotNull(result.externalMessageId());
        assertTrue(result.externalMessageId().startsWith("FAKE_"), result.externalMessageId());
        assertTrue(result.externalMessageId().contains("1234567890"));
    }

    @Test
    void send_shouldGenerateMonotonicIncreasingWamids() {
        WhatsAppProvider.SendResult a = provider.send(request());
        WhatsAppProvider.SendResult b = provider.send(request());
        assertTrue(!a.externalMessageId().equals(b.externalMessageId()));
    }

    private WhatsAppProvider.SendRequest request() {
        return new WhatsAppProvider.SendRequest(UUID.randomUUID(), UUID.randomUUID(),
                "42", "+5511999990000", "teste", null);
    }
}