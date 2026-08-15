package com.becommerce.crm.application.omnichannel.port.output;

import com.becommerce.crm.domain.omnichannel.Conversation;
import com.becommerce.crm.application.identity.dto.PageResponse;

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
