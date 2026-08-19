package com.becommerce.crm.application.ai.port.output;

import com.becommerce.crm.domain.ai.AiAction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistencia de acoes de escrita propostas pelo assistente de IA (AI-05).
 * Todos os acessos sao scoped a empresa ativa (RLS); a posse por usuario e
 * validada em dominio/service. {@link #findByIdForUpdate} adquire lock
 * pessimista para tornar a transicao PROPOSED -> terminal atomica sob
 * confirmacoes concorrentes.
 */
public interface AiActionRepository {

    AiAction save(AiAction action);

    Optional<AiAction> findById(UUID id);

    Optional<AiAction> findByIdForUpdate(UUID id);

    List<AiAction> findByConversationId(UUID conversationId);
}