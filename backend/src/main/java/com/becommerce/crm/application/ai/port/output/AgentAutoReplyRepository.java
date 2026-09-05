package com.becommerce.crm.application.ai.port.output;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Registro de auto-respostas do agente (tabela {@code agent_auto_replies}, V070).
 *
 * <p>Serve de guarda de idempotência (no máximo 1 resposta por mensagem entrante)
 * e de janela de cooldown (respeitando {@code AgentConfig.cooldownMinutes}). O
 * reserva da resposta é feita ANTES da chamada à IA: se falhar, não há resposta
 * duplicada — apenas nenhuma resposta para aquela mensagem.</p>
 */
public interface AgentAutoReplyRepository {

    /**
     * Reserva idempotente de uma auto-resposta para a mensagem entrante. Retorna
     * {@code false} se já existir uma reserva para {@code (companyId, inboundMessageId)}.
     */
    boolean reserve(UUID companyId, UUID conversationId, UUID inboundMessageId);

    /** Data da última auto-resposta na conversa (vazio se nunca respondeu). */
    Optional<LocalDateTime> lastAutoReplyAt(UUID companyId, UUID conversationId);
}