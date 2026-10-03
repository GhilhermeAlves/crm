package com.becommerce.crm.communication.omnichannel.application.port.output;

import com.becommerce.crm.communication.omnichannel.domain.Channel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de saída para canais omnichannel (RLS FORCE via GUC). */
public interface OmnichannelChannelRepository {

    Channel save(Channel channel);

    Optional<Channel> findById(UUID id);

    Optional<Channel> findByCompanyAndExternalId(UUID companyId, String externalId);

    List<Channel> findByCompanyId(UUID companyId);

    void delete(Channel channel);
}
