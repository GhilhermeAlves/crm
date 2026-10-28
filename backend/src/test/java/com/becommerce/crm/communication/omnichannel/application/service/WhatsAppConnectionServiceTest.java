package com.becommerce.crm.communication.omnichannel.application.service;

import com.becommerce.crm.communication.omnichannel.application.dto.ChannelRequest;
import com.becommerce.crm.communication.omnichannel.application.dto.ChannelResponse;
import com.becommerce.crm.communication.omnichannel.application.port.input.OmnichannelChannelUseCase;
import com.becommerce.crm.communication.omnichannel.application.port.output.WhatsAppProvider;
import com.becommerce.crm.communication.omnichannel.domain.ChannelProvider;
import com.becommerce.crm.communication.omnichannel.domain.ChannelStatus;
import com.becommerce.crm.communication.omnichannel.domain.ChannelType;
import com.becommerce.crm.communication.omnichannel.domain.OmnichannelProviderException;
import com.becommerce.crm.communication.omnichannel.infrastructure.whatsapp.EvolutionInstanceClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WhatsAppConnectionServiceTest {

    private final EvolutionInstanceClient evolution = mock(EvolutionInstanceClient.class);
    private final OmnichannelChannelUseCase channels = mock(OmnichannelChannelUseCase.class);
    private final WhatsAppProvider provider = mock(WhatsAppProvider.class);
    private final WhatsAppConnectionService service = new WhatsAppConnectionService(evolution, channels, provider);
    private final UUID companyId = UUID.randomUUID();
    private final String instance = "crm-" + companyId;

    private ChannelResponse channel(ChannelStatus status) {
        return new ChannelResponse(UUID.randomUUID(), companyId, ChannelType.WHATSAPP, ChannelProvider.EVOLUTION,
                "WhatsApp", status, instance, null, null, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void firstConnectionCreatesChannelAndInstanceAndReturnsQr() {
        when(evolution.isConfigured()).thenReturn(true);
        when(channels.listByCompany(companyId)).thenReturn(List.of());
        when(channels.create(eq(companyId), any())).thenReturn(channel(ChannelStatus.ACTIVE));
        when(evolution.state(instance)).thenReturn("not_found");
        when(evolution.connect(instance)).thenReturn(new EvolutionInstanceClient.QrCode("data:image/png;base64,QR", null));

        WhatsAppConnectionService.ConnectionQr qr = service.connect(companyId);

        ArgumentCaptor<ChannelRequest> request = ArgumentCaptor.forClass(ChannelRequest.class);
        verify(channels).create(eq(companyId), request.capture());
        assertThat(request.getValue().externalId()).isEqualTo(instance);
        assertThat(request.getValue().provider()).isEqualTo(ChannelProvider.EVOLUTION);
        verify(evolution).create(instance);
        assertThat(qr.state()).isEqualTo("CONNECTING");
        assertThat(qr.qrCode()).isEqualTo("data:image/png;base64,QR");
    }

    @Test
    void alreadyConnectedDoesNotAskForQr() {
        when(evolution.isConfigured()).thenReturn(true);
        when(channels.listByCompany(companyId)).thenReturn(List.of(channel(ChannelStatus.ACTIVE)));
        when(evolution.state(instance)).thenReturn("open");

        assertThat(service.connect(companyId).state()).isEqualTo("CONNECTED");
        verify(evolution, never()).connect(any());
        verify(evolution, never()).create(any());
    }

    @Test
    void statusReflectsConfigurationAndEvolutionState() {
        when(evolution.isConfigured()).thenReturn(false);
        assertThat(service.status(companyId).state()).isEqualTo("NOT_CONFIGURED");

        when(evolution.isConfigured()).thenReturn(true);
        when(channels.listByCompany(companyId)).thenReturn(List.of());
        assertThat(service.status(companyId).state()).isEqualTo("NOT_CREATED");

        when(channels.listByCompany(companyId)).thenReturn(List.of(channel(ChannelStatus.ACTIVE)));
        when(evolution.state(instance)).thenReturn("close");
        assertThat(service.status(companyId).state()).isEqualTo("DISCONNECTED");
    }

    @Test
    void sendTextGoesThroughTheConnectedInstanceWithCountryCode() {
        when(channels.listByCompany(companyId)).thenReturn(List.of(channel(ChannelStatus.ACTIVE)));
        when(evolution.state(instance)).thenReturn("open");

        service.sendText(companyId, "(11) 98888-7777", "Olá");

        ArgumentCaptor<WhatsAppProvider.SendRequest> sent = ArgumentCaptor.forClass(WhatsAppProvider.SendRequest.class);
        verify(provider).send(sent.capture());
        assertThat(sent.getValue().to()).isEqualTo("5511988887777");
        assertThat(sent.getValue().phoneNumberId()).isEqualTo(instance);
    }

    @Test
    void sendTextRequiresConnectionAndPhone() {
        assertThatThrownBy(() -> service.sendText(companyId, null, "x"))
                .hasMessageContaining("celular");
        when(channels.listByCompany(companyId)).thenReturn(List.of(channel(ChannelStatus.ACTIVE)));
        when(evolution.state(instance)).thenReturn("close");
        assertThatThrownBy(() -> service.sendText(companyId, "11988887777", "x"))
                .hasMessageContaining("não está conectado");
    }

    @Test
    void connectWithoutServerConfigurationFailsClearly() {
        when(evolution.isConfigured()).thenReturn(false);
        assertThatThrownBy(() -> service.connect(companyId))
                .isInstanceOf(OmnichannelProviderException.class)
                .hasMessageContaining("EVOLUTION_BASE_URL");
    }
}
