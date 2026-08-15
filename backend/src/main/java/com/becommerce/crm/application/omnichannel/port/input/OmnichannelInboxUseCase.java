package com.becommerce.crm.application.omnichannel.port.input;

import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.application.omnichannel.dto.ConversationDetailResponse;
import com.becommerce.crm.application.omnichannel.dto.ConversationResponse;
import com.becommerce.crm.application.omnichannel.dto.MessageResponse;

import java.util.UUID;

/** Inbox omnichannel: conversas, mensagens e envio (FASE 7/8/9/11). */
public interface OmnichannelInboxUseCase {

    PageResponse<ConversationResponse> listConversations(UUID companyId, int page, int pageSize);

    ConversationDetailResponse getConversation(UUID companyId, UUID conversationId, int page, int pageSize);

    MessageResponse send(UUID companyId, UUID conversationId, String body);

    void markRead(UUID companyId, UUID conversationId);
}