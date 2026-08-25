package com.becommerce.crm.application.campaign.port.output;

import java.util.UUID;

/**
 * Abstração de despacho por canal (PLAN.md seção 6). A campanha NÃO conhece o
 * provider: implementações declaram suporte via {@link #supports(String)} e a
 * execução escolhe a primeira compatível com o channel_type da campanha.
 */
public interface CampaignChannelDispatcher {

    boolean supports(String channelType);

    SendResult send(SendCommand command);

    record SendCommand(UUID companyId, UUID providerChannelId, String to, String body) {}

    record SendResult(String externalMessageId) {}

    class DispatchException extends RuntimeException {
        public DispatchException(String message) {
            super(message);
        }
    }
}
