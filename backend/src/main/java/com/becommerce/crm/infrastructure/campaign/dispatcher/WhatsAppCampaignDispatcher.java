package com.becommerce.crm.infrastructure.campaign.dispatcher;

import com.becommerce.crm.application.campaign.port.output.CampaignChannelDispatcher;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelChannelRepository;
import com.becommerce.crm.application.omnichannel.port.output.WhatsAppProvider;
import com.becommerce.crm.domain.omnichannel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Dispatcher de campanhas para WhatsApp (Sprint 17). Reutiliza o provider da
 * Sprint 16 ({@link WhatsAppProvider}) — sem duplicar auth/HMAC/secrets.
 * Throttling é aplicado pelo executor entre envios (campaign.dispatch.throttle-ms).
 */
@Component
public class WhatsAppCampaignDispatcher implements CampaignChannelDispatcher {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppCampaignDispatcher.class);

    private final WhatsAppProvider whatsAppProvider;
    private final OmnichannelChannelRepository channelRepository;

    public WhatsAppCampaignDispatcher(WhatsAppProvider whatsAppProvider,
                                      OmnichannelChannelRepository channelRepository) {
        this.whatsAppProvider = whatsAppProvider;
        this.channelRepository = channelRepository;
    }

    @Override
    public boolean supports(String channelType) {
        return "WHATSAPP".equalsIgnoreCase(channelType);
    }

    @Override
    public SendResult send(SendCommand command) {
        Channel channel = channelRepository.findById(command.providerChannelId())
                .orElseThrow(() -> new DispatchException("Canal não encontrado."));
        if (!channel.getCompanyId().equals(command.companyId())) {
            // defense-in-depth: canal de outro tenant é tratado como inexistente
            throw new DispatchException("Canal não encontrado.");
        }
        try {
            WhatsAppProvider.SendResult result = whatsAppProvider.send(new WhatsAppProvider.SendRequest(
                    command.companyId(), channel.getId(), channel.getExternalId(),
                    command.to(), command.body()));
            log.info("Campanha: mensagem enviada company={} to={}", command.companyId(), command.to());
            return new SendResult(result != null ? result.externalMessageId() : null);
        } catch (RuntimeException e) {
            throw new DispatchException(e.getMessage());
        }
    }
}
