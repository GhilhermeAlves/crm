package com.becommerce.crm.communication.omnichannel.application.dto;

import com.becommerce.crm.communication.omnichannel.domain.ChannelProvider;
import com.becommerce.crm.communication.omnichannel.domain.ChannelStatus;
import com.becommerce.crm.communication.omnichannel.domain.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Criação/atualização de canal. {@code secretsRef} é uma referência (nunca o valor do token). */
public record ChannelRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull ChannelType type,
        @NotNull ChannelProvider provider,
        String externalId,
        String config,
        String secretsRef,
        ChannelStatus status
) {
}