package com.becommerce.crm.application.omnichannel.dto;

import com.becommerce.crm.domain.omnichannel.ChannelProvider;
import com.becommerce.crm.domain.omnichannel.ChannelStatus;
import com.becommerce.crm.domain.omnichannel.ChannelType;

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