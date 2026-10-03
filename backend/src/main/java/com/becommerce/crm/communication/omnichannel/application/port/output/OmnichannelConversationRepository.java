package com.becommerce.crm.communication.omnichannel.application.port.output;

import com.becommerce.crm.communication.omnichannel.domain.Conversation;
import com.becommerce.crm.shared.application.dto.PageResponse;

import java.util.Optional;
import java.util.UUID;

/** Porta de saída para conversas omnichannel (RLS FORCE via GUC). */
public interface OmnichannelConversationRepository {

    Conversation save(Conversation conversation);

    Optional<Conversation> findById(UUID id);

    Optional<Conversation> findByCompanyAndChannelAndPhone(UUID companyId, UUID channelId, String externalPhone);

    PageResponse<Conversation> findByCompany(UUID companyId, int page, int pageSize);

    long countUnreadByCompany(UUID companyId);
}
