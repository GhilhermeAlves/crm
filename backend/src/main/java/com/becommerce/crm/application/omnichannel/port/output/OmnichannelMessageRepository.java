package com.becommerce.crm.application.omnichannel.port.output;

import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.domain.omnichannel.Message;
import com.becommerce.crm.domain.omnichannel.MessageStatus;

import java.util.Optional;
import java.util.UUID;

/** Porta de saída para mensagens omnichannel (RLS FORCE via GUC). */
public interface OmnichannelMessageRepository {

    Message save(Message message);

    /** Upsert por chave externa (idempotência do provedor) — retorna a mensagem persistida. */
    Message saveByExternalId(Message message);

    Optional<Message> findById(UUID id);

    Optional<Message> findByExternalMessageId(String externalId);

    Optional<Message> findByClientMessageId(UUID clientMessageId);

    PageResponse<Message> findByConversation(UUID conversationId, int page, int pageSize);

    /** Corpo da última mensagem da conversa (para a lista do Inbox). */
    Optional<String> findLastBodyByConversation(UUID conversationId);

    /** Atualiza status e erro de uma mensagem identificada por id externo. */
    void updateStatusByExternalId(String externalId, MessageStatus status, String error);
}