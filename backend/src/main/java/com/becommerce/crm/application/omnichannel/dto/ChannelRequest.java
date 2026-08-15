package com.becommerce.crm.application.omnichannel.dto;

import com.becommerce.crm.domain.omnichannel.ChannelProvider;
import com.becommerce.crm.domain.omnichannel.ChannelStatus;
import com.becommerce.crm.domain.omnichannel.ChannelType;
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