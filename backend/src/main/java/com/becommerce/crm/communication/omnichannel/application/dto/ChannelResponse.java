package com.becommerce.crm.communication.omnichannel.application.dto;

import com.becommerce.crm.communication.omnichannel.domain.ChannelProvider;
import com.becommerce.crm.communication.omnichannel.domain.ChannelStatus;
import com.becommerce.crm.communication.omnichannel.domain.ChannelType;

import java.time.LocalDateTime;
import java.util.UUID;

/** Canal no formato de resposta da API (sem secrets — apenas a referência). */
public record ChannelResponse(
        UUID id,
        UUID companyId,
        ChannelType type,
        ChannelProvider provider,
        String name,
        ChannelStatus status,
        String externalId,
        String config,
        String secretsRef,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}