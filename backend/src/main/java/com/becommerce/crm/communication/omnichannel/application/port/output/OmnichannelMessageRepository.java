package com.becommerce.crm.communication.omnichannel.application.port.output;

import com.becommerce.crm.shared.application.dto.PageResponse;
import com.becommerce.crm.communication.omnichannel.domain.Message;
import com.becommerce.crm.communication.omnichannel.domain.MessageStatus;

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

    /** As {@code limit} mensagens MAIS RECENTES da conversa, em ordem cronológica (contexto da IA). */
    java.util.List<Message> findRecentByConversation(UUID conversationId, int limit);

    /** Corpo da última mensagem da conversa (para a lista do Inbox). */
    Optional<String> findLastBodyByConversation(UUID conversationId);

    /**
     * Existe mensagem INBOUND do cliente depois de {@code after} na conversa?
     * Usada pelo processador de FollowUp (Sprint 22) para invalidar follow-up
     * obsoleto quando o cliente respondeu depois que ele foi criado.
     */
    boolean existsInboundAfter(UUID conversationId, java.time.LocalDateTime after);

    /** Houve qualquer OUTBOUND na conversa depois de {@code after}? */
    boolean existsOutboundAfter(UUID conversationId, java.time.LocalDateTime after);

    /** Substitui o corpo da mensagem (ex.: transcrição do áudio ou resumo da imagem/PDF). */
    void updateBody(UUID messageId, String body);

    /** O CRM enviou (OUTBOUND) este mesmo texto na conversa depois de {@code after}? */
    boolean existsOutboundWithBodyAfter(UUID conversationId, String body, java.time.LocalDateTime after);

    /** Atualiza status e erro de uma mensagem identificada por id externo, escopada ao tenant. */
    void updateStatusByExternalId(UUID companyId, String externalId, MessageStatus status, String error);
}