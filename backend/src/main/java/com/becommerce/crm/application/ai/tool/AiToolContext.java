package com.becommerce.crm.application.ai.tool;

import com.becommerce.crm.application.ai.context.AiPermissionContext;

import java.util.UUID;

/**
 * Contexto de execucao de uma Tool (AI-03). E SEMPRE derivado do
 * {@code CurrentUser} autenticado pelo backend - nunca de argumentos vindos do
 * LLM. O modelo nao e autoridade sobre tenant, usuario ou permissoes.
 *
 * <p>AI-05: {@code conversationId} identifica a conversa em curso (resolvida
 * pelo backend) para vincular propostas de escrita a ela. Read tools ignoram.
 *
 * @param companyId empresa ativa (do CurrentUser)
 * @param userId usuario autenticado (do CurrentUser)
 * @param permissions permissoes do usuario (do CurrentUser)
 * @param conversationId conversa em curso (resolvida pelo backend), pode ser null
 */
public record AiToolContext(UUID companyId, UUID userId, AiPermissionContext permissions,
                            UUID conversationId) {

    public AiToolContext(UUID companyId, UUID userId, AiPermissionContext permissions) {
        this(companyId, userId, permissions, null);
    }
}