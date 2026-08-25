package com.becommerce.crm.application.campaign.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Vincula canal (Omnichannel) + template à campanha (etapa Canal/Mensagem do wizard). */
public record AttachChannelRequest(
        @Size(max = 20) String channelType,
        @NotNull UUID providerChannelId,
        @NotNull UUID templateId
) {
}
