package com.becommerce.crm.communication.omnichannel.infrastructure.whatsapp;

import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelChannelRepository;
import com.becommerce.crm.communication.omnichannel.application.port.output.WhatsAppProvider;
import com.becommerce.crm.communication.omnichannel.domain.Channel;
import com.becommerce.crm.communication.omnichannel.domain.ChannelProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoutingWhatsAppProviderTest {

    private final MetaCloudWhatsAppProvider meta = mock(MetaCloudWhatsAppProvider.class);
    private final WhatsAppProvider evolution = mock(WhatsAppProvider.class);
    private final OmnichannelChannelRepository channels = mock(OmnichannelChannelRepository.class);
    private final RoutingWhatsAppProvider routing =
            new RoutingWhatsAppProvider(meta, List.of(meta, evolution), channels);

    private WhatsAppProvider.SendRequest requestFor(ChannelProvider provider) {
        UUID channelId = UUID.randomUUID();
        Channel channel = mock(Channel.class);
        when(channel.getProvider()).thenReturn(provider);
        when(channels.findById(channelId)).thenReturn(Optional.of(channel));
        return new WhatsAppProvider.SendRequest(UUID.randomUUID(), channelId, "ref", "5511", "Oi", null);
    }

    @Test
    void canalMetaCloud_vaiParaMeta() {
        WhatsAppProvider.SendRequest request = requestFor(ChannelProvider.META_CLOUD);
        when(meta.send(request)).thenReturn(new WhatsAppProvider.SendResult("wamid"));

        assertEquals("wamid", routing.send(request).externalMessageId());
        verify(evolution, never()).send(request);
    }

    @Test
    void canalEvolution_vaiParaProviderConfigurado() {
        WhatsAppProvider.SendRequest request = requestFor(ChannelProvider.EVOLUTION);
        when(evolution.send(request)).thenReturn(new WhatsAppProvider.SendResult("3EB0"));

        assertEquals("3EB0", routing.send(request).externalMessageId());
        verify(meta, never()).send(request);
    }

    @Test
    void canalNaoEncontrado_usaProviderConfigurado() {
        WhatsAppProvider.SendRequest request =
                new WhatsAppProvider.SendRequest(UUID.randomUUID(), UUID.randomUUID(), "ref", "5511", "Oi", null);
        when(channels.findById(request.channelId())).thenReturn(Optional.empty());
        when(evolution.send(request)).thenReturn(new WhatsAppProvider.SendResult("x"));

        assertEquals("x", routing.send(request).externalMessageId());
    }
}
