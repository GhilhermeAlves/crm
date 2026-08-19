package com.becommerce.crm.application.ai.tool;

import com.becommerce.crm.application.ai.context.AiPermissionContext;

import java.util.UUID;

/**
 * Contexto de execução de uma Tool (AI-03). É SEMPRE derivado do
 * {@code CurrentUser} autenticado pelo backend — nunca de argumentos vindos do
 * LLM. O modelo não é autoridade sobre tenant, usuário ou permissões.
 *
 * @param companyId empresa ativa (do CurrentUser)
 * @param userId usuário autenticado (do CurrentUser)
 * @param permissions permissões do usuário (do CurrentUser)
 */
public record AiToolContext(UUID companyId, UUID userId, AiPermissionContext permissions) {
}