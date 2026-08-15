package com.becommerce.crm.application.omnichannel.port.input;

import com.becommerce.crm.application.omnichannel.dto.ChannelRequest;
import com.becommerce.crm.application.omnichannel.dto.ChannelResponse;
import com.becommerce.crm.domain.omnichannel.ChannelStatus;

import java.util.List;
import java.util.UUID;

/** Gestão de canais omnichannel (FASE 3) scoped à empresa ativa. */
public interface OmnichannelChannelUseCase {

    ChannelResponse create(UUID companyId, ChannelRequest request);

    ChannelResponse getById(UUID companyId, UUID channelId);

    List<ChannelResponse> listByCompany(UUID companyId);

    ChannelResponse update(UUID companyId, UUID channelId, ChannelRequest request);

    ChannelResponse setStatus(UUID companyId, UUID channelId, ChannelStatus status);

    void delete(UUID companyId, UUID channelId);
}