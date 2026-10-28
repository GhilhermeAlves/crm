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
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Conexão do WhatsApp da empresa por QR code (Evolution API). A empresa tem um
 * canal WHATSAPP/EVOLUTION cujo {@code externalId} é o nome da instância
 * ({@code crm-<companyId>}); o webhook da instância já aponta para o CRM.
 */
@Service
public class WhatsAppConnectionService {

    /** Situação para a tela: NOT_CONFIGURED, NOT_CREATED, CONNECTED, CONNECTING ou DISCONNECTED. */
    public record ConnectionStatus(String state, String instance, UUID channelId) {
    }

    /** QR code a ser lido pelo celular (data URL) e código de pareamento opcional. */
    public record ConnectionQr(String state, String qrCode, String pairingCode) {
    }

    private final EvolutionInstanceClient evolution;
    private final OmnichannelChannelUseCase channels;
    private final WhatsAppProvider provider;

    public WhatsAppConnectionService(EvolutionInstanceClient evolution, OmnichannelChannelUseCase channels,
                                     WhatsAppProvider provider) {
        this.evolution = evolution;
        this.channels = channels;
        this.provider = provider;
    }

    /** True se a empresa tem o WhatsApp conectado (instância aberta). */
    public boolean isConnected(UUID companyId) {
        return "CONNECTED".equals(status(companyId).state());
    }

    /**
     * Mensagem avulsa pelo WhatsApp conectado da empresa (ex.: link do formulário).
     * Não abre conversa no Inbox; quem chama registra o envio onde fizer sentido.
     */
    public void sendText(UUID companyId, String phone, String text) {
        String to = normalizePhone(phone);
        if (to == null) {
            throw new OmnichannelProviderException("O paciente não tem celular cadastrado.");
        }
        ChannelResponse channel = evolutionChannel(companyId)
                .orElseThrow(() -> new OmnichannelProviderException("WhatsApp da clínica não conectado."));
        if (!"open".equals(evolution.state(channel.externalId()))) {
            throw new OmnichannelProviderException("WhatsApp da clínica não está conectado.");
        }
        provider.send(new WhatsAppProvider.SendRequest(companyId, channel.id(), channel.externalId(), to, text,
                channel.secretsRef()));
    }

    /** Só dígitos; número nacional (até 11 dígitos) ganha o DDI 55. */
    static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 10) {
            return null;
        }
        return digits.length() <= 11 ? "55" + digits : digits;
    }

    static String instanceName(UUID companyId) {
        return "crm-" + companyId;
    }

    public ConnectionStatus status(UUID companyId) {
        if (!evolution.isConfigured()) {
            return new ConnectionStatus("NOT_CONFIGURED", null, null);
        }
        Optional<ChannelResponse> channel = evolutionChannel(companyId);
        if (channel.isEmpty()) {
            return new ConnectionStatus("NOT_CREATED", null, null);
        }
        String instance = channel.get().externalId();
        return new ConnectionStatus(map(evolution.state(instance)), instance, channel.get().id());
    }

    /** Cria a instância e o canal se ainda não existem e devolve o QR code para ler. */
    public ConnectionQr connect(UUID companyId) {
        if (!evolution.isConfigured()) {
            throw new OmnichannelProviderException(
                    "WhatsApp não configurado no servidor (EVOLUTION_BASE_URL / EVOLUTION_API_KEY).");
        }
        ChannelResponse channel = evolutionChannel(companyId).orElseGet(() -> channels.create(companyId,
                new ChannelRequest("WhatsApp", ChannelType.WHATSAPP, ChannelProvider.EVOLUTION,
                        instanceName(companyId), null, null, ChannelStatus.ACTIVE)));
        String instance = channel.externalId();
        String state = evolution.state(instance);
        if ("not_found".equals(state)) {
            evolution.create(instance);
        } else if ("open".equals(state)) {
            return new ConnectionQr("CONNECTED", null, null);
        }
        if (channel.status() != ChannelStatus.ACTIVE) {
            channels.setStatus(companyId, channel.id(), ChannelStatus.ACTIVE);
        }
        EvolutionInstanceClient.QrCode qr = evolution.connect(instance);
        return new ConnectionQr("CONNECTING", qr.base64(), qr.pairingCode());
    }

    public void disconnect(UUID companyId) {
        evolutionChannel(companyId).ifPresent(channel -> evolution.logout(channel.externalId()));
    }

    private Optional<ChannelResponse> evolutionChannel(UUID companyId) {
        return channels.listByCompany(companyId).stream()
                .filter(c -> c.type() == ChannelType.WHATSAPP && c.provider() == ChannelProvider.EVOLUTION)
                .filter(c -> c.externalId() != null && !c.externalId().isBlank())
                .findFirst();
    }

    static String map(String evolutionState) {
        return switch (evolutionState == null ? "" : evolutionState) {
            case "open" -> "CONNECTED";
            case "connecting" -> "CONNECTING";
            case "not_found" -> "NOT_CREATED";
            default -> "DISCONNECTED";
        };
    }
}
