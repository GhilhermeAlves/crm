package com.becommerce.crm.communication.omnichannel.infrastructure.whatsapp;

import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelChannelRepository;
import com.becommerce.crm.communication.omnichannel.application.port.output.WhatsAppProvider;
import com.becommerce.crm.communication.omnichannel.domain.Channel;
import com.becommerce.crm.communication.omnichannel.domain.ChannelProvider;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Escolhe o provider pelo canal: {@code META_CLOUD} vai para a Cloud API da Meta;
 * os demais seguem o provider configurado em {@code omnichannel.whatsapp.provider}
 * (Evolution ou fake). Assim canais Evolution e Meta convivem na mesma instalação.
 */
@Primary
@Component
public class RoutingWhatsAppProvider implements WhatsAppProvider {

    private final MetaCloudWhatsAppProvider meta;
    private final WhatsAppProvider configured;
    private final OmnichannelChannelRepository channelRepository;

    /** {@code providers} não inclui este bean (auto-referência é excluída pelo Spring). */
    public RoutingWhatsAppProvider(MetaCloudWhatsAppProvider meta, List<WhatsAppProvider> providers,
                                   OmnichannelChannelRepository channelRepository) {
        this.meta = meta;
        this.configured = providers.stream()
                .filter(p -> p != meta && !(p instanceof RoutingWhatsAppProvider))
                .findFirst()
                .orElse(meta);
        this.channelRepository = channelRepository;
    }

    @Override
    public SendResult send(SendRequest request) {
        return byChannel(request.channelId() == null ? Optional.empty()
                : channelRepository.findById(request.channelId())).send(request);
    }

    @Override
    public SendResult sendVoice(SendRequest request, byte[] audio) {
        return byChannel(request.channelId() == null ? Optional.empty()
                : channelRepository.findById(request.channelId())).sendVoice(request, audio);
    }

    @Override
    public Optional<MediaContent> downloadMedia(String instance, String externalMessageId, String secretsRef) {
        Optional<Channel> channel = TenantContext.hasCompanyId() && instance != null
                ? channelRepository.findByCompanyAndExternalId(TenantContext.getCompanyId(), instance)
                : Optional.empty();
        return byChannel(channel).downloadMedia(instance, externalMessageId, secretsRef);
    }

    @Override
    public String providerName() {
        return configured.providerName() + "+META_CLOUD";
    }

    private WhatsAppProvider byChannel(Optional<Channel> channel) {
        return channel.map(Channel::getProvider).filter(ChannelProvider.META_CLOUD::equals).isPresent()
                ? meta : configured;
    }
}
